package org.springframework.cloud.client.loadbalancer.reactive;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.ClientRequestContext;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * @author Olga Maciaszek-Sharma
 */
public class RetryableLoadBalancerExchangeFilterFunction implements ExchangeFilterFunction {

	private static final Log LOG = LogFactory
			.getLog(RetryableLoadBalancerExchangeFilterFunction.class);
	private static final List<Class<? extends Throwable>> exceptions = Arrays
			.asList(IOException.class, TimeoutException.class,
					org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class);
	private final LoadBalancerRetryPolicy retryPolicy;
	private final LoadBalancerProperties properties;
	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	public RetryableLoadBalancerExchangeFilterFunction(LoadBalancerRetryPolicy retryPolicy,
			LoadBalancerProperties properties, ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
		this.retryPolicy = retryPolicy;
		this.properties = properties;
		this.loadBalancerFactory = loadBalancerFactory;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction next) {
		LoadBalancerRetryContext loadBalancerRetryContext = new LoadBalancerRetryContext(clientRequest);
		Retry exchangeRetry = buildRetrySpec(properties.getRetry()
				.getMaxRetriesOnSameServiceInstance(), true);
		Retry filterRetry = buildRetrySpec(properties.getRetry()
				.getMaxRetriesOnNextServiceInstance(), false);

		URI originalUrl = clientRequest.url();
		String serviceId = originalUrl.getHost();
		if (serviceId == null) {
			String message = String
					.format("Request URI does not contain a valid hostname: %s", originalUrl
							.toString());
			if (LOG.isWarnEnabled()) {
				LOG.warn(message);
			}
			return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).body(message)
					.build());
		}
		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
				.getSupportedLifecycleProcessors(
						loadBalancerFactory
								.getInstances(serviceId, LoadBalancerLifecycle.class),
						ClientRequestContext.class, ClientResponse.class, ServiceInstance.class);
		String hint = getHint(serviceId);
		DefaultRequest<RetryableRequestContext> lbRequest = new DefaultRequest<>(
				new RetryableRequestContext(null, clientRequest, hint));
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
		return Mono.defer(() -> choose(serviceId, lbRequest).flatMap(lbResponse -> {
					ServiceInstance instance = lbResponse.getServer();
					lbRequest
							.setContext(new RetryableRequestContext(instance, clientRequest, hint));
					if (instance == null) {
						String message = serviceInstanceUnavailableMessage(serviceId);
						if (LOG.isWarnEnabled()) {
							LOG.warn(message);
						}
						supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
								.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbResponse)));
						return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
								.body(serviceInstanceUnavailableMessage(serviceId)).build());
					}

					if (LOG.isDebugEnabled()) {
						LOG.debug(String
								.format("LoadBalancer has retrieved the instance for service %s: %s", serviceId,
										instance.getUri()));
					}
					ClientRequest newRequest = buildClientRequest(clientRequest, reconstructURI(instance, originalUrl));
					return next.exchange(newRequest)
							.doOnError(throwable -> supportedLifecycleProcessors.forEach(
									lifecycle -> lifecycle
											.onComplete(new CompletionContext<ClientResponse, ServiceInstance>(
													CompletionContext.Status.FAILED, throwable, lbResponse))))
							.doOnSuccess(clientResponse ->
									supportedLifecycleProcessors.forEach(
											lifecycle -> lifecycle
													.onComplete(new CompletionContext<ClientResponse, ServiceInstance>(
															CompletionContext.Status.SUCCESS, lbResponse, clientResponse))))
							.map(clientResponse -> {
								loadBalancerRetryContext.setClientResponse(clientResponse);
								if (shouldRetrySameServiceInstance(loadBalancerRetryContext)) {
									if (LOG.isDebugEnabled()) {
										LOG.debug(String
												.format("Retrying on status code: %d", clientResponse
														.statusCode().value()));
									}
									throw new RetryableStatusCodeException();
								}
								return clientResponse;

							});
				})
						.map(clientResponse -> {
									loadBalancerRetryContext.setClientResponse(clientResponse);
									if (shouldRetryNextServiceInstance(loadBalancerRetryContext)) {
										if (LOG.isDebugEnabled()) {
											LOG.debug(String
													.format("Retrying on status code: %d", clientResponse
															.statusCode().value()));
										}
										throw new RetryableStatusCodeException();
									}
									return clientResponse;

								}
						)
						.retryWhen(exchangeRetry)
		)
				.retryWhen(filterRetry);
	}

	@NotNull
	private Retry buildRetrySpec(int max, boolean transientErrors) {
		LoadBalancerProperties.Retry.Backoff backoffProperties = properties.getRetry()
				.getBackoff();
		if (backoffProperties.isEnabled()) {
			return RetrySpec
					.backoff(max, backoffProperties.getMinBackoff())
					.filter(this::isRetryException)
					.maxBackoff(backoffProperties.getMaxBackoff())
					.jitter(backoffProperties.getJitter())
					.transientErrors(transientErrors);
		}
		return RetrySpec
				.max(max)
				.filter(this::isRetryException)
				.transientErrors(transientErrors);
	}

	private boolean shouldRetrySameServiceInstance(LoadBalancerRetryContext loadBalancerRetryContext) {
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

	private boolean shouldRetryNextServiceInstance(LoadBalancerRetryContext loadBalancerRetryContext) {
		boolean shouldRetry = retryPolicy
				.retryableStatusCode(loadBalancerRetryContext.getResponseStatusCode()) &&
				retryPolicy.canRetryOnMethod(loadBalancerRetryContext.getRequestMethod())
				&& retryPolicy.canRetryNextServiceInstance(loadBalancerRetryContext);
		if (shouldRetry) {
			loadBalancerRetryContext.incrementRetriesNextServiceInstance();
			loadBalancerRetryContext.resetRetriesSameServiceInstance();
		}
		return shouldRetry;
	}

	private boolean isRetryException(Throwable throwable) {
		return exceptions.stream()
				.anyMatch(exception -> exception
						.isInstance(throwable) || throwable != null && exception
						.isInstance(throwable.getCause())
						|| throwable.getClass().getName()
						.contains("RetryExhaustedException"));
	}

	protected URI reconstructURI(ServiceInstance instance, URI original) {
		return LoadBalancerUriTools.reconstructURI(instance, original);
	}

	protected Mono<Response<ServiceInstance>> choose(String serviceId, Request<RetryableRequestContext> request) {
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

	private String getHint(String serviceId) {
		String defaultHint = properties.getHint().getOrDefault("default", "default");
		String hintPropertyValue = properties.getHint().get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}
}
