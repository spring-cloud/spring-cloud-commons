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
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.reconstructURI;
import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.buildClientRequest;

/**
 * An {@link ExchangeFilterFunction} that uses {@link ReactiveLoadBalancer} to execute
 * requests against a correct {@link ServiceInstance} and Reactor Retries to retry the
 * call both against the same and the next service instance, based on the provided
 * {@link LoadBalancerRetryPolicy}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
public class RetryableLoadBalancerExchangeFilterFunction
		implements ExchangeFilterFunction {

	private static final Log LOG = LogFactory
			.getLog(RetryableLoadBalancerExchangeFilterFunction.class);

	private static final List<Class<? extends Throwable>> exceptions = Arrays.asList(
			IOException.class, TimeoutException.class,
			RetryableStatusCodeException.class);

	private final LoadBalancerRetryPolicy retryPolicy;

	private final LoadBalancerRetryProperties retryProperties;

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	private final List<LoadBalancerClientRequestTransformer> transformers;

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy, ReactiveLoadBalancer.Factory, LoadBalancerRetryProperties, List)}.
	 * @param retryPolicy the retry policy
	 * @param loadBalancerFactory the loadbalancer factory
	 * @param retryProperties the retry properties
	 */
	@Deprecated
	public RetryableLoadBalancerExchangeFilterFunction(
			LoadBalancerRetryPolicy retryPolicy,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
			LoadBalancerRetryProperties retryProperties) {
		this(retryPolicy, loadBalancerFactory, retryProperties, Collections.emptyList());
	}

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy, ReactiveLoadBalancer.Factory, LoadBalancerRetryProperties, List)}.
	 * @param loadBalancerFactory the loadbalancer factory
	 * @param retryProperties the retry properties
	 */
	@Deprecated
	public RetryableLoadBalancerExchangeFilterFunction(
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
			LoadBalancerRetryProperties retryProperties) {
		this(new RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(retryProperties),
				loadBalancerFactory, retryProperties);
	}

	public RetryableLoadBalancerExchangeFilterFunction(
			LoadBalancerRetryPolicy retryPolicy,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
			LoadBalancerRetryProperties retryProperties,
			List<LoadBalancerClientRequestTransformer> transformers) {
		this.retryPolicy = retryPolicy;
		this.loadBalancerFactory = loadBalancerFactory;
		this.retryProperties = retryProperties;
		this.transformers = transformers;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Mono<ClientResponse> filter(ClientRequest clientRequest,
			ExchangeFunction next) {
		LoadBalancerRetryContext loadBalancerRetryContext = new LoadBalancerRetryContext(
				clientRequest);
		Retry exchangeRetry = buildRetrySpec(
				retryProperties.getMaxRetriesOnSameServiceInstance(), true);
		Retry filterRetry = buildRetrySpec(
				retryProperties.getMaxRetriesOnNextServiceInstance(), false);

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
		DefaultRequest<RetryableRequestContext> lbRequest = new DefaultRequest<>(
				new RetryableRequestContext(null));
		return Mono.defer(() -> choose(serviceId, lbRequest).flatMap(lbResponse -> {
			ServiceInstance instance = lbResponse.getServer();
			lbRequest.setContext(new RetryableRequestContext(instance));
			if (instance == null) {
				String message = "LoadBalancer does not contain an instance for the service "
						+ serviceId;
				if (LOG.isWarnEnabled()) {
					LOG.warn("LoadBalancer does not contain an instance for the service "
							+ serviceId);
				}
				return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
						.body(message).build());
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format(
						"LoadBalancer has retrieved the instance for service %s: %s",
						serviceId, instance.getUri()));
			}
			ClientRequest newRequest = buildClientRequest(clientRequest,
					reconstructURI(instance, originalUrl), instance, transformers);
			return next.exchange(newRequest).map(clientResponse -> {
				loadBalancerRetryContext.setClientResponse(clientResponse);
				if (shouldRetrySameServiceInstance(loadBalancerRetryContext)) {
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
			if (shouldRetryNextServiceInstance(loadBalancerRetryContext)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrying on status code: %d",
							clientResponse.statusCode().value()));
				}
				throw new RetryableStatusCodeException();
			}
			return clientResponse;

		}).retryWhen(exchangeRetry)).retryWhen(filterRetry);
	}

	private Retry buildRetrySpec(int max, boolean transientErrors) {
		LoadBalancerRetryProperties.Backoff backoffProperties = retryProperties
				.getBackoff();
		if (backoffProperties.isEnabled()) {
			return RetrySpec.backoff(max, backoffProperties.getMinBackoff())
					.filter(this::isRetryException)
					.maxBackoff(backoffProperties.getMaxBackoff())
					.jitter(backoffProperties.getJitter())
					.transientErrors(transientErrors);
		}
		return RetrySpec.max(max).filter(this::isRetryException)
				.transientErrors(transientErrors);
	}

	private boolean shouldRetrySameServiceInstance(
			LoadBalancerRetryContext loadBalancerRetryContext) {
		boolean shouldRetry = retryPolicy
				.retryableStatusCode(loadBalancerRetryContext.getResponseStatusCode())
				&& retryPolicy
						.canRetryOnMethod(loadBalancerRetryContext.getRequestMethod())
				&& retryPolicy.canRetrySameServiceInstance(loadBalancerRetryContext);
		if (shouldRetry) {
			loadBalancerRetryContext.incrementRetriesSameServiceInstance();
		}
		return shouldRetry;
	}

	private boolean shouldRetryNextServiceInstance(
			LoadBalancerRetryContext loadBalancerRetryContext) {
		boolean shouldRetry = retryPolicy
				.retryableStatusCode(loadBalancerRetryContext.getResponseStatusCode())
				&& retryPolicy
						.canRetryOnMethod(loadBalancerRetryContext.getRequestMethod())
				&& retryPolicy.canRetryNextServiceInstance(loadBalancerRetryContext);
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

	protected Mono<Response<ServiceInstance>> choose(String serviceId,
			Request<RetryableRequestContext> request) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory
				.getInstance(serviceId);
		if (loadBalancer == null) {
			return Mono.just(
					new org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse());
		}
		return Mono.from(loadBalancer.choose(request));
	}

}
