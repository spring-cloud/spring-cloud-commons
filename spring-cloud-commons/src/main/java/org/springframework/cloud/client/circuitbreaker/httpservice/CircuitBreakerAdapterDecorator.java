/*
 * Copyright 2013-2025 the original author or authors.
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

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapterDecorator;
import org.springframework.web.service.invoker.HttpRequestValues;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.getFallback;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerAdapterDecorator extends HttpExchangeAdapterDecorator {

	private static final Log LOG = LogFactory.getLog(CircuitBreakerAdapterDecorator.class);

	private final CircuitBreaker circuitBreaker;

	private final Class<?> fallbackClass;

	private volatile Object fallbackProxy;

	public CircuitBreakerAdapterDecorator(HttpExchangeAdapter delegate, CircuitBreaker circuitBreaker,
			Class<?> fallbackClass) {
		super(delegate);
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

	// Visible for tests
	CircuitBreaker getCircuitBreaker() {
		return circuitBreaker;
	}

	// Visible for tests
	Class<?> getFallbackClass() {
		return fallbackClass;
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
		return throwable -> getFallback(requestValues, throwable, getFallbackProxy(), fallbackClass);
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
