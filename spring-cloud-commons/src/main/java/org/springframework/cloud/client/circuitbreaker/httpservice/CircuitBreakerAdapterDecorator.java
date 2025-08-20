/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapterDecorator;
import org.springframework.web.service.invoker.HttpRequestValues;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.createProxies;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.getFallback;

/**
 * Blocking implementation of {@link HttpExchangeAdapterDecorator} that wraps
 * {@code @HttpExchange}
 * <p>
 * In the event of a CircuitBreaker fallback, this class uses the user-provided fallback
 * class to create a proxy. The fallback method is selected by matching either:
 * <ul>
 * <li>A method with the same name and argument types as the original method, or</li>
 * <li>A method with the same name and the original arguments preceded by a
 * {@link Throwable}, allowing the user to access the cause of failure within the
 * fallback.</li>
 * </ul>
 * Once a matching method is found, it is invoked to provide the fallback behavior. Both
 * the fallback class and the fallback methods must be public.
 * </p>
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see HttpServiceFallback
 */
public class CircuitBreakerAdapterDecorator extends HttpExchangeAdapterDecorator {

	private static final Log LOG = LogFactory.getLog(CircuitBreakerAdapterDecorator.class);

	private final CircuitBreaker circuitBreaker;

	private final Map<String, Class<?>> fallbackClasses;

	private volatile Map<String, Object> fallbackProxies;

	public CircuitBreakerAdapterDecorator(HttpExchangeAdapter delegate, CircuitBreaker circuitBreaker,
			Map<String, Class<?>> fallbackClasses) {
		super(delegate);
		this.circuitBreaker = circuitBreaker;
		this.fallbackClasses = fallbackClasses;
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

	// Visible for tests
	CircuitBreaker getCircuitBreaker() {
		return circuitBreaker;
	}

	// Visible for tests
	Map<String, Class<?>> getFallbackClasses() {
		return fallbackClasses;
	}

	@SuppressWarnings("unchecked")
	private <T> T castIfPossible(Object result) {
		try {
			return (T) result;
		}
		catch (ClassCastException exception) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Failed to cast object of type " + result.getClass() + " to expected type.");
			}
			throw exception;
		}
	}

	// Visible for tests
	Function<Throwable, Object> createFallbackHandler(HttpRequestValues requestValues) {
		return throwable -> getFallback(requestValues, throwable, getFallbackProxies(), fallbackClasses);
	}

	private Map<String, Object> getFallbackProxies() {
		if (fallbackProxies == null) {
			synchronized (this) {
				if (fallbackProxies == null) {
					fallbackProxies = createProxies(fallbackClasses);
				}
			}
		}
		return fallbackProxies;
	}

}
