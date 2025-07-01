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

package org.springframework.cloud.client.circuitbreaker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapterDecorator;
import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerRestClientAdapterDecorator extends HttpExchangeAdapterDecorator {

	private static final Log LOG = LogFactory.getLog(CircuitBreakerRestClientAdapterDecorator.class);

	private final CircuitBreaker circuitBreaker;

	private final Class<?> fallbackClass;

	private volatile Object fallbackProxy;

	public CircuitBreakerRestClientAdapterDecorator(HttpExchangeAdapter delegate, CircuitBreaker circuitBreaker,
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

	private Function<Throwable, Object> createFallbackHandler(HttpRequestValues requestValues) {
		Map<String, Object> attributes = requestValues.getAttributes();
		Method fallback = resolveFallbackMethod(attributes, false);
		Method fallbackWithCause = resolveFallbackMethod(attributes, true);

		return throwable -> {
			if (fallback != null) {
				return invokeFallback(fallback, attributes, null);
			}
			else if (fallbackWithCause != null) {
				return invokeFallback(fallbackWithCause, attributes, throwable);
			}
			else {
				throw new NoFallbackAvailableException("No fallback available.", throwable);
			}
		};

	}

	private Method resolveFallbackMethod(Map<String, Object> attributes, boolean withThrowable) {
		if (fallbackClass == null) {
			return null;
		}
		String methodName = String.valueOf(attributes.get(CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME));
		Class<?>[] paramTypes = (Class<?>[]) attributes
			.get(CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME);
		Class<?>[] effectiveTypes = withThrowable
				? Stream.concat(Stream.of(Throwable.class), Arrays.stream(paramTypes)).toArray(Class[]::new)
				: paramTypes;

		try {
			Method method = fallbackClass.getMethod(methodName, effectiveTypes);
			method.setAccessible(true);
			return method;
		}
		catch (NoSuchMethodException exception) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Fallback method not found: " + methodName + " in " + fallbackClass.getName(), exception);
			}
			return null;
		}
	}

	private Object invokeFallback(Method method, Map<String, Object> attributes, @Nullable Throwable throwable) {
		try {
			Object proxy = getFallbackProxy();
			Object[] args = (Object[]) attributes.get(CircuitBreakerRequestValueProcessor.ARGUMENTS_ATTRIBUTE_NAME);
			Object[] finalArgs = (throwable != null)
					? Stream.concat(Stream.of(throwable), Arrays.stream(args)).toArray(Object[]::new) : args;
			return method.invoke(proxy, finalArgs);
		}
		catch (InvocationTargetException | IllegalAccessException exception) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Error invoking fallback method: " + method.getName(), exception);
			}
			throw new RuntimeException("Failed to invoke fallback method", exception);
		}
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
