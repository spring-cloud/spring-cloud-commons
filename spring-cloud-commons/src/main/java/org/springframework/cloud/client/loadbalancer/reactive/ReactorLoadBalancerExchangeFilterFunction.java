/*
 * Copyright 2012-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext.Status;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * An {@link ExchangeFilterFunction} that uses {@link ReactiveLoadBalancer} to execute
 * requests against a correct {@link ServiceInstance}. Implements
 * {@link ApplicationEventPublisherAware} in order to publish events with retrieved
 * {@link Response}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class ReactorLoadBalancerExchangeFilterFunction
		implements ExchangeFilterFunction, ApplicationEventPublisherAware {

	private static final Log LOG = LogFactory
			.getLog(ReactorLoadBalancerExchangeFilterFunction.class);

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	private ApplicationEventPublisher eventPublisher;

	public ReactorLoadBalancerExchangeFilterFunction(
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
		this.loadBalancerFactory = loadBalancerFactory;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction next) {
		URI originalUrl = clientRequest.url();
		String serviceId = originalUrl.getHost();
		if (serviceId == null) {
			String message = String.format(
					"Request URI does not contain a valid hostname: %s",
					originalUrl.toString());
			if (LOG.isWarnEnabled()) {
				LOG.warn(message);
			}
			return Mono.just(
					ClientResponse.create(HttpStatus.BAD_REQUEST).body(message).build());
		}
		DefaultRequest<DefaultRequestContext> lbRequest = new DefaultRequest<>(new DefaultRequestContext(clientRequest));
		return choose(serviceId, lbRequest).flatMap(lbResponse -> {
			if (!lbResponse.hasServer()) {
				String message = serviceInstanceUnavailableMessage(serviceId);
				if (LOG.isWarnEnabled()) {
					LOG.warn(message);
				}
				completeAndPublishLoadBalancerResponse(lbResponse,
						new CompletionContext(Status.DISCARD, clientRequest));
				return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
						.body(serviceInstanceUnavailableMessage(serviceId)).build());
			}
			ServiceInstance instance = lbResponse.getServer();

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format(
						"Load balancer has retrieved the instance for service %s: %s",
						serviceId, instance.getUri()));
			}
			ClientRequest newRequest = buildClientRequest(clientRequest,
					reconstructURI(instance, originalUrl));
			return next.exchange(newRequest)
					.doOnError(throwable -> completeAndPublishLoadBalancerResponse(
							lbResponse, new CompletionContext(Status.FAILED, clientRequest, throwable)))
					.doOnSuccess(clientResponse -> {
						completeAndPublishLoadBalancerResponse(
								lbResponse, new CompletionContext(Status.SUCCESS, clientRequest, clientResponse));
						if (lbResponse
								.getContext() instanceof StickySessionResponseContext)
							clientResponse.cookies()
									.add("affinity_key", ResponseCookie
											.from("affinity_key", ((StickySessionResponseContext) lbResponse
													.getContext()).getAffinityKey())
											.build());
					});
		});
	}

	private void completeAndPublishLoadBalancerResponse(
			Response<ServiceInstance, DefaultResponseContext> response, CompletionContext completionContext) {
		response.onComplete(completionContext);
		eventPublisher.publishEvent(new LoadBalancerResponseEvent(response));
	}

	protected URI reconstructURI(ServiceInstance instance, URI original) {
		return LoadBalancerUriTools.reconstructURI(instance, original);
	}

	protected Mono<Response<ServiceInstance, DefaultResponseContext>> choose(String serviceId, Request<DefaultRequestContext> request) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory
				.getInstance(serviceId);
		if (loadBalancer == null) {
			return Mono.just(new EmptyResponse());
		}
		return Mono.from(loadBalancer.choose(request));
	}

	private String serviceInstanceUnavailableMessage(String serviceId) {
		return "Load balancer does not contain an instance for the service " + serviceId;
	}

	private ClientRequest buildClientRequest(ClientRequest request, URI uri) {
		return ClientRequest.create(request.method(), uri)
				.headers(headers -> headers.addAll(request.headers()))
				.cookies(cookies -> cookies.addAll(request.cookies()))
				.attributes(attributes -> attributes.putAll(request.attributes()))
				.body(request.body()).build();
	}

	@Override
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		eventPublisher = applicationEventPublisher;
	}

}
