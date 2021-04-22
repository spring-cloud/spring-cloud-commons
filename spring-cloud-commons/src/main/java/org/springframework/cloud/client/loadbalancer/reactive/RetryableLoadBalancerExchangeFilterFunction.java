/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerPropertiesFactory;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
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
 * requests against a correct {@link ServiceInstance} and Reactor Retries to retry the
 * call both against the same and the next service instance, based on the provided
 * {@link LoadBalancerRetryPolicy}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @since 3.0.0
 */
public class RetryableLoadBalancerExchangeFilterFunction implements LoadBalancedExchangeFilterFunction {

	private static final Log LOG = LogFactory.getLog(RetryableLoadBalancerExchangeFilterFunction.class);

	private static final List<Class<? extends Throwable>> exceptions = Arrays.asList(IOException.class,
			TimeoutException.class, RetryableStatusCodeException.class);

	private final LoadBalancerRetryPolicy retryPolicy;

	private final LoadBalancerProperties properties;

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	private final List<LoadBalancerClientRequestTransformer> transformers;

	private LoadBalancerPropertiesFactory propertiesFactory;

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy, ReactiveLoadBalancer.Factory, LoadBalancerProperties, List)}.
	 */
	@Deprecated
	public RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy retryPolicy,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory, LoadBalancerProperties properties) {
		this(retryPolicy, loadBalancerFactory, properties, Collections.emptyList());
	}

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy, ReactiveLoadBalancer.Factory, LoadBalancerProperties, List, LoadBalancerPropertiesFactory)}
	 */
	@Deprecated
	public RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy retryPolicy,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory, LoadBalancerProperties properties,
			List<LoadBalancerClientRequestTransformer> transformers) {
		this.retryPolicy = retryPolicy;
		this.loadBalancerFactory = loadBalancerFactory;
		this.properties = properties;
		this.transformers = transformers;
	}

	public RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy retryPolicy,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory, LoadBalancerProperties properties,
			List<LoadBalancerClientRequestTransformer> transformers, LoadBalancerPropertiesFactory propertiesFactory) {
		this(retryPolicy, loadBalancerFactory, properties, transformers);
		this.propertiesFactory = propertiesFactory;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

		LoadBalancerRetryContext loadBalancerRetryContext = new LoadBalancerRetryContext(clientRequest);
		Retry exchangeRetry = buildRetrySpec(serviceId,
				getLoadBalancerProperties(serviceId).getRetry().getMaxRetriesOnSameServiceInstance(), true);
		Retry filterRetry = buildRetrySpec(serviceId,
				getLoadBalancerProperties(serviceId).getRetry().getMaxRetriesOnNextServiceInstance(), false);

		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
				.getSupportedLifecycleProcessors(
						loadBalancerFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
						RetryableRequestContext.class, ResponseData.class, ServiceInstance.class);
		String hint = getHint(serviceId, getLoadBalancerProperties(serviceId).getHint());
		RequestData requestData = new RequestData(clientRequest);
		DefaultRequest<RetryableRequestContext> lbRequest = new DefaultRequest<>(
				new RetryableRequestContext(null, requestData, hint));
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
		return Mono.defer(() -> choose(serviceId, lbRequest).flatMap(lbResponse -> {
			ServiceInstance instance = lbResponse.getServer();
			lbRequest.setContext(new RetryableRequestContext(instance, requestData, hint));
			if (instance == null) {
				String message = serviceInstanceUnavailableMessage(serviceId);
				if (LOG.isWarnEnabled()) {
					LOG.warn(message);
				}
				supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
						.onComplete(new CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>(
								CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
				return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
						.body(serviceInstanceUnavailableMessage(serviceId)).build());
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("LoadBalancer has retrieved the instance for service %s: %s", serviceId,
						instance.getUri()));
			}
			LoadBalancerProperties.StickySession stickySessionProperties = getLoadBalancerProperties(serviceId)
					.getStickySession();
			ClientRequest newRequest = buildClientRequest(clientRequest, instance,
					stickySessionProperties.getInstanceIdCookieName(),
					stickySessionProperties.isAddServiceInstanceCookie(), transformers);
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, lbResponse));
			return next.exchange(newRequest)
					.doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
							.onComplete(new CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>(
									CompletionContext.Status.FAILED, throwable, lbRequest, lbResponse))))
					.doOnSuccess(clientResponse -> supportedLifecycleProcessors.forEach(
							lifecycle -> lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS,
									lbRequest, lbResponse, new ResponseData(clientResponse, requestData)))))
					.map(clientResponse -> {
						loadBalancerRetryContext.setClientResponse(clientResponse);
						if (shouldRetrySameServiceInstance(serviceId, loadBalancerRetryContext)) {
							if (LOG.isDebugEnabled()) {
								LOG.debug(String.format("Retrying on status code: %d",
										clientResponse.statusCode().value()));
							}
							throw new RetryableStatusCodeException();
						}
						return clientResponse;

					});
		}).map(clientResponse -> {
			loadBalancerRetryContext.setClientResponse(clientResponse);
			if (shouldRetryNextServiceInstance(serviceId, loadBalancerRetryContext)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrying on status code: %d", clientResponse.statusCode().value()));
				}
				throw new RetryableStatusCodeException();
			}
			return clientResponse;

		}).retryWhen(exchangeRetry)).retryWhen(filterRetry);
	}

	private Retry buildRetrySpec(String serviceId, int max, boolean transientErrors) {
		LoadBalancerProperties.Retry.Backoff backoffProperties = getLoadBalancerProperties(serviceId).getRetry()
				.getBackoff();
		if (backoffProperties.isEnabled()) {
			return RetrySpec.backoff(max, backoffProperties.getMinBackoff()).filter(this::isRetryException)
					.maxBackoff(backoffProperties.getMaxBackoff()).jitter(backoffProperties.getJitter())
					.transientErrors(transientErrors);
		}
		return RetrySpec.max(max).filter(this::isRetryException).transientErrors(transientErrors);
	}

	private boolean shouldRetrySameServiceInstance(String serviceId,
			LoadBalancerRetryContext loadBalancerRetryContext) {
		boolean shouldRetry = retryPolicy.retryableStatusCode(serviceId,
				loadBalancerRetryContext.getResponseStatusCode())
				&& retryPolicy.canRetryOnMethod(serviceId, loadBalancerRetryContext.getRequestMethod())
				&& retryPolicy.canRetrySameServiceInstance(serviceId, loadBalancerRetryContext);
		if (shouldRetry) {
			loadBalancerRetryContext.incrementRetriesSameServiceInstance();
		}
		return shouldRetry;
	}

	private boolean shouldRetryNextServiceInstance(String serviceId,
			LoadBalancerRetryContext loadBalancerRetryContext) {
		boolean shouldRetry = retryPolicy.retryableStatusCode(serviceId,
				loadBalancerRetryContext.getResponseStatusCode())
				&& retryPolicy.canRetryOnMethod(serviceId, loadBalancerRetryContext.getRequestMethod())
				&& retryPolicy.canRetryNextServiceInstance(serviceId, loadBalancerRetryContext);
		if (shouldRetry) {
			loadBalancerRetryContext.incrementRetriesNextServiceInstance();
			loadBalancerRetryContext.resetRetriesSameServiceInstance();
		}
		return shouldRetry;
	}

	private boolean isRetryException(Throwable throwable) {
		return exceptions.stream()
				.anyMatch(exception -> exception.isInstance(throwable)
						|| throwable != null && exception.isInstance(throwable.getCause())
						|| Exceptions.isRetryExhausted(throwable));
	}

	protected Mono<Response<ServiceInstance>> choose(String serviceId, Request<RetryableRequestContext> request) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory.getInstance(serviceId);
		if (loadBalancer == null) {
			return Mono.just(new EmptyResponse());
		}
		return Mono.from(loadBalancer.choose(request));
	}

	@Deprecated
	private LoadBalancerProperties getLoadBalancerProperties(String serviceId) {
		if (propertiesFactory != null) {
			return propertiesFactory.getLoadBalancerProperties(serviceId);
		} else {
			return properties;
		}
	}

}
