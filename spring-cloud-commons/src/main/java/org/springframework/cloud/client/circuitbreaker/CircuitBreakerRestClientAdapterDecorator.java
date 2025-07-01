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
import java.util.Map;
import java.util.function.Function;

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

	private final Class<?> fallbacks;

	private Object fallbackProxy;

	public CircuitBreakerRestClientAdapterDecorator(HttpExchangeAdapter delegate, CircuitBreaker circuitBreaker,
			Class<?> fallbacks) {
		super(delegate);
		this.circuitBreaker = circuitBreaker;
		this.fallbacks = fallbacks;
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		circuitBreaker.run(() -> {
			super.exchange(requestValues);
			return null;
		}, handleThrowable(requestValues));
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		return (HttpHeaders) circuitBreaker.run(() -> super.exchangeForHeaders(values), handleThrowable(values));
	}

	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		Object result = circuitBreaker.run(() -> super.exchangeForBody(values, bodyType), handleThrowable(values));
		return handleCast(result);
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		Object result = circuitBreaker.run(() -> super.exchangeForBodilessEntity(values), handleThrowable(values));
		return handleCast(result);
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		Object result = circuitBreaker.run(() -> super.exchangeForEntity(values, bodyType), handleThrowable(values));
		return handleCast(result);
	}

	@SuppressWarnings("unchecked")
	private <T> T handleCast(Object result) {
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

	private Function<Throwable, Object> handleThrowable(HttpRequestValues requestValues) {
		Map<String, Object> attributes = requestValues.getAttributes();
		Method fallbackMethod = getFallbackMethod(attributes);

		return throwable -> {
			try {
				if (fallbackMethod == null) {
					throw new NoFallbackAvailableException("No fallback available.", throwable);
				}
				Object fallbackProxy = getFallbackProxy();
				Object[] arguments = (Object[]) attributes
					.get(CircuitBreakerRequestValueProcessor.ARGUMENTS_ATTRIBUTE_NAME);
				return fallbackMethod.invoke(fallbackProxy, arguments);
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error("Could not invoke fallback method " + fallbackMethod.getName() + " due to exception: "
							+ e.getMessage(), e);
				}
				throw new RuntimeException(e);
			}
			catch (NoSuchMethodException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error("Default constructor not found in: " + fallbacks.getName()
							+ ". Fallback class needs to have a default constructor", e);
				}
				throw new RuntimeException(e);
			}
			catch (InstantiationException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error("Could not instantiate fallback class: " + fallbacks.getName(), e);
				}
				throw new RuntimeException(e);
			}
		};
	}

	private Method getFallbackMethod(Map<String, Object> attributes) {
		if (fallbacks == null) {
			return null;
		}
		String methodName = String.valueOf(attributes.get(CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME));
		Class<?>[] parameterTypes = (Class<?>[]) attributes
			.get(CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME);
		Method method;
		try {
			method = fallbacks.getMethod(methodName, parameterTypes);
			method.setAccessible(true);
		}
		catch (NoSuchMethodException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Could not find fallback method " + methodName + " in class " + fallbacks.getName(), e);
			}
			throw new RuntimeException(e);
		}
		return method;
	}

	private Object getFallbackProxy()
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (fallbackProxy == null) {
			Object target = fallbacks.getConstructor().newInstance();
			ProxyFactory proxyFactory = new ProxyFactory(target);
			proxyFactory.setProxyTargetClass(true);
			fallbackProxy = proxyFactory.getProxy();
		}
		return fallbackProxy;
	}

}
