package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapter;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapterDecorator;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.castIfPossible;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.getFallback;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ReactiveCircuitBreakerAdapterDecorator extends ReactorHttpExchangeAdapterDecorator {

	private static final Log LOG = LogFactory.getLog(ReactiveCircuitBreakerAdapterDecorator.class);

	private final ReactiveCircuitBreaker reactiveCircuitBreaker;

	private final CircuitBreaker circuitBreaker;

	private final Class<?> fallbackClass;

	private volatile Object fallbackProxy;

	public ReactiveCircuitBreakerAdapterDecorator(ReactorHttpExchangeAdapter delegate,
			ReactiveCircuitBreaker reactiveCircuitBreaker, CircuitBreaker circuitBreaker,
			Class<?> fallbackClass) {
		super(delegate);
		this.reactiveCircuitBreaker = reactiveCircuitBreaker;
		this.circuitBreaker = circuitBreaker;
		this.fallbackClass = fallbackClass;
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		circuitBreaker.run(() -> {
			super.exchange(requestValues);
			return null;
		}, createFallbackHandler(requestValues));
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		Object result = circuitBreaker.run(() -> super.exchangeForHeaders(values), createFallbackHandler(values));
		return castIfPossible(result);
	}

	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		Object result = circuitBreaker.run(() -> super.exchangeForBody(values, bodyType),
				createFallbackHandler(values));
		return castIfPossible(result);
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		Object result = circuitBreaker.run(() -> super.exchangeForBodilessEntity(values),
				createFallbackHandler(values));
		return castIfPossible(result);
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		Object result = circuitBreaker.run(() -> super.exchangeForEntity(values, bodyType),
				createFallbackHandler(values));
		return castIfPossible(result);
	}

	public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
		return reactiveCircuitBreaker.run(
				super.exchangeForMono(requestValues),
				createMonoFallbackHandler(requestValues));
	}

	public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
		return reactiveCircuitBreaker.run(super.exchangeForHeadersMono(requestValues),
				createHttpHeadersFallbackHandler(requestValues));
	}

	public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return reactiveCircuitBreaker.run(super.exchangeForBodyMono(requestValues, bodyType),
				createMonoFallbackHandler(requestValues));
	}

	public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return reactiveCircuitBreaker.run(super.exchangeForBodyFlux(requestValues, bodyType),
				createFluxFallbackHandler(requestValues));
	}

	public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
		return reactiveCircuitBreaker.run(super.exchangeForBodilessEntityMono(requestValues),
				createMonoFallbackHandler(requestValues));
	}

	public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return reactiveCircuitBreaker.run(super.exchangeForEntityMono(requestValues, bodyType),
				createMonoFallbackHandler(requestValues));
	}

	public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return reactiveCircuitBreaker.run(super.exchangeForEntityFlux(requestValues, bodyType),
				createMonoFallbackHandler(requestValues));
	}

	// Visible for tests
	Function<Throwable, Object> createFallbackHandler(HttpRequestValues requestValues) {
		return throwable -> getFallback(requestValues, throwable, getFallbackProxy(), fallbackClass);
	}

	<T> Function<Throwable, Mono<T>> createMonoFallbackHandler(HttpRequestValues requestValues) {
		return throwable -> {
			T fallback = castIfPossible(getFallback(requestValues, throwable, getFallbackProxy(), fallbackClass));
			return Mono.just(fallback);
		};
	}

	<T> Function<Throwable, Flux<T>> createFluxFallbackHandler(HttpRequestValues requestValues) {
		return throwable -> {
			T fallback = castIfPossible(getFallback(requestValues, throwable, getFallbackProxy(), fallbackClass));
			return Flux.just(fallback);
		};
	}

	Function<Throwable, Mono<HttpHeaders>> createHttpHeadersFallbackHandler(HttpRequestValues requestValues) {
		return throwable -> {
			HttpHeaders fallback = castIfPossible(getFallback(requestValues, throwable, getFallbackProxy(), fallbackClass));
			return Mono.just(fallback);
		};
	}

	// Visible for tests
	ReactiveCircuitBreaker getReactiveCircuitBreaker() {
		return reactiveCircuitBreaker;
	}

	// Visible for tests
	CircuitBreaker getCircuitBreaker() {
		return circuitBreaker;
	}

	// Visible for tests
	Class<?> getFallbackClass() {
		return fallbackClass;
	}

	private Object getFallbackProxy() {
		if (fallbackProxy == null) {
			synchronized (this) {
				if (fallbackProxy == null) {
					try {
						Object target = fallbackClass.getConstructor().newInstance();
						ProxyFactory proxyFactory = new ProxyFactory(target);
						proxyFactory.setProxyTargetClass(true);
						fallbackProxy = proxyFactory.getProxy();
					}
					catch (ReflectiveOperationException exception) {
						if (LOG.isErrorEnabled()) {
							LOG.error("Error instantiating fallback proxy for class: " + fallbackClass.getName(),
									exception);
						}
						throw new RuntimeException("Could not create fallback proxy", exception);
					}
				}
			}
		}
		return fallbackProxy;
	}

}
