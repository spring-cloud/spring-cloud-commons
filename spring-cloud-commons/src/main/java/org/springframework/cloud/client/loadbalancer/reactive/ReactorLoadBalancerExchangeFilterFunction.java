/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer.reactive;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.buildClientRequest;
import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.getHint;
import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.serviceInstanceUnavailableMessage;

/**
 * An {@link ExchangeFilterFunction} that uses {@link ReactiveLoadBalancer} to execute
 * requests against a correct {@link ServiceInstance}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ReactorLoadBalancerExchangeFilterFunction implements LoadBalancedExchangeFilterFunction {

	private static final Log LOG = LogFactory.getLog(ReactorLoadBalancerExchangeFilterFunction.class);

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	private final List<LoadBalancerClientRequestTransformer> transformers;

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #ReactorLoadBalancerExchangeFilterFunction(ReactiveLoadBalancer.Factory, LoadBalancerProperties, List)}.
	 * @param loadBalancerFactory the loadbalancer factory
	 * @param properties the properties for SC LoadBalancer
	 */
	@Deprecated
	public ReactorLoadBalancerExchangeFilterFunction(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
			LoadBalancerProperties properties) {
		this(loadBalancerFactory, properties, Collections.emptyList());
	}

	/**
	 * @deprecated in favour of
	 * {@link ReactorLoadBalancerExchangeFilterFunction#ReactorLoadBalancerExchangeFilterFunction(ReactiveLoadBalancer.Factory, List)}
	 */
	@Deprecated
	public ReactorLoadBalancerExchangeFilterFunction(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
			LoadBalancerProperties properties, List<LoadBalancerClientRequestTransformer> transformers) {
		this.loadBalancerFactory = loadBalancerFactory;
		this.transformers = transformers;
	}

	public ReactorLoadBalancerExchangeFilterFunction(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
			List<LoadBalancerClientRequestTransformer> transformers) {
		this.loadBalancerFactory = loadBalancerFactory;
		this.transformers = transformers;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction next) {
		URI originalUrl = clientRequest.url();
		String serviceId = originalUrl.getHost();
		if (serviceId == null) {
			String message = String.format("Request URI does not contain a valid hostname: %s", originalUrl.toString());
			if (LOG.isWarnEnabled()) {
				LOG.warn(message);
			}
			return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).body(message).build());
		}
		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
				.getSupportedLifecycleProcessors(
						loadBalancerFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
						RequestDataContext.class, ResponseData.class, ServiceInstance.class);
		String hint = getHint(serviceId, loadBalancerFactory.getProperties(serviceId).getHint());
		RequestData requestData = new RequestData(clientRequest);
		DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData, hint));
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
		return choose(serviceId, lbRequest).flatMap(lbResponse -> {
			ServiceInstance instance = lbResponse.getServer();
			if (instance == null) {
				String message = serviceInstanceUnavailableMessage(serviceId);
				if (LOG.isWarnEnabled()) {
					LOG.warn(message);
				}
				supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
						.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
				return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
						.body(serviceInstanceUnavailableMessage(serviceId)).build());
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("LoadBalancer has retrieved the instance for service %s: %s", serviceId,
						instance.getUri()));
			}
			LoadBalancerProperties properties = loadBalancerFactory.getProperties(serviceId);
			LoadBalancerProperties.StickySession stickySessionProperties = properties.getStickySession();
			ClientRequest newRequest = buildClientRequest(clientRequest, instance,
					stickySessionProperties.getInstanceIdCookieName(),
					stickySessionProperties.isAddServiceInstanceCookie(), transformers);
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, lbResponse));
			return next.exchange(newRequest)
					.doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
							.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
									CompletionContext.Status.FAILED, throwable, lbRequest, lbResponse))))
					.doOnSuccess(clientResponse -> supportedLifecycleProcessors.forEach(
							lifecycle -> lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS,
									lbRequest, lbResponse, buildResponseData(requestData, clientResponse,
											properties.isUseRawStatusCodeInResponseData())))));
		});
	}

	private ResponseData buildResponseData(RequestData requestData, ClientResponse clientResponse,
			boolean useRawStatusCodes) {
		if (useRawStatusCodes) {
			return new ResponseData(requestData, clientResponse);
		}
		return new ResponseData(clientResponse, requestData);
	}

	protected Mono<Response<ServiceInstance>> choose(String serviceId, Request<RequestDataContext> request) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory.getInstance(serviceId);
		if (loadBalancer == null) {
			return Mono.just(new EmptyResponse());
		}
		return Mono.from(loadBalancer.choose(request));
	}

}
