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

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.util.UriBuilderFactory;

/**
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
// TODO: change into decorator after support in FW added
@SuppressWarnings("unchecked")
public final class CircuitBreakerRestClientAdapter implements HttpExchangeAdapter {

	// FIXME: get fallbacks from factory

	private static final Log LOG = LogFactory.getLog(CircuitBreakerRestClientAdapter.class);

	private final RestClient restClient;

	private final CircuitBreaker circuitBreaker;

	private final Class<?> fallbacks;

	private Object fallbackProxy;

	private CircuitBreakerRestClientAdapter(RestClient restClient, CircuitBreaker circuitBreaker,
			// TODO: generics
			Class<?> fallbacks) {
		this.restClient = restClient;
		this.circuitBreaker = circuitBreaker;
		this.fallbacks = fallbacks;
	}

	@Override
	public boolean supportsRequestAttributes() {
		return true;
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		circuitBreaker.run(() -> newRequest(requestValues).retrieve().toBodilessEntity(),
				handleThrowable(requestValues));
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		return (HttpHeaders) circuitBreaker.run(() -> newRequest(values).retrieve()
						.toBodilessEntity().getHeaders(),
				handleThrowable(values));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return (T) circuitBreaker.run(() -> {
			if (bodyType.getType().equals(InputStream.class)) {
				return (T) newRequest(values).exchange((request, response) -> response.getBody(), false);
			}
			return newRequest(values).retrieve().body(bodyType);
		}, handleThrowable(values));
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		return (ResponseEntity<Void>) circuitBreaker.run(() -> newRequest(values).retrieve()
						.toBodilessEntity(),
				handleThrowable(values));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return (ResponseEntity<T>) circuitBreaker.run(() -> {
			if (bodyType.getType().equals(InputStream.class)) {
				return newRequest(values)
						.exchangeForRequiredValue((request, response) -> ResponseEntity.status(response.getStatusCode())
								.headers(response.getHeaders())
								.body(response.getBody()), false);
			}
			return newRequest(values).retrieve().toEntity(bodyType);
		}, handleThrowable(values));
	}

	@SuppressWarnings("unchecked")
	private <B> RestClient.RequestBodySpec newRequest(HttpRequestValues values) {

		HttpMethod httpMethod = values.getHttpMethod();
		Assert.notNull(httpMethod, "HttpMethod is required");

		RestClient.RequestBodyUriSpec uriSpec = this.restClient.method(httpMethod);

		RestClient.RequestBodySpec bodySpec;
		if (values.getUri() != null) {
			bodySpec = uriSpec.uri(values.getUri());
		}
		else if (values.getUriTemplate() != null) {
			UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
			if (uriBuilderFactory != null) {
				URI uri = uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables());
				bodySpec = uriSpec.uri(uri);
			}
			else {
				bodySpec = uriSpec.uri(values.getUriTemplate(), values.getUriVariables());
			}
		}
		else {
			throw new IllegalStateException("Neither full URL nor URI template");
		}

		bodySpec.headers(headers -> headers.putAll(values.getHeaders()));

		if (!values.getCookies().isEmpty()) {
			List<String> cookies = new ArrayList<>();
			values.getCookies()
					.forEach((name, cookieValues) -> cookieValues.forEach(value -> {
						HttpCookie cookie = new HttpCookie(name, value);
						cookies.add(cookie.toString());
					}));
			bodySpec.header(HttpHeaders.COOKIE, String.join("; ", cookies));
		}

		if (values.getApiVersion() != null) {
			bodySpec.apiVersion(values.getApiVersion());
		}

		bodySpec.attributes(attributes -> attributes.putAll(values.getAttributes()));

		B body = (B) values.getBodyValue();
		if (body != null) {
			if (body instanceof StreamingHttpOutputMessage.Body streamingBody) {
				bodySpec.body(streamingBody);
			}
			else if (values.getBodyValueType() != null) {
				bodySpec.body(body, (ParameterizedTypeReference<? super B>) values.getBodyValueType());
			}
			else {
				bodySpec.body(body);
			}
		}
		return bodySpec;
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

	/**
	 * Create a {@link RestClientAdapter} for the given {@link RestClient}.
	 */
	public static CircuitBreakerRestClientAdapter create(RestClient restClient, CircuitBreaker circuitBreaker,
			Class<?> fallbacks) {
		return new CircuitBreakerRestClientAdapter(restClient, circuitBreaker, fallbacks);
	}

}
