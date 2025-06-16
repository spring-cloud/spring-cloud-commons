package org.springframework.cloud.client.circuitbreaker;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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
public class CircuitBreakerRestClientAdapter implements HttpExchangeAdapter {

	// FIXME: get fallbacks

	private final RestClient restClient;
	private final CircuitBreaker circuitBreaker;
	private final Class<?> fallbacks;

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
		Map<String, Object> attributes = requestValues.getAttributes();
		String methodName = String.valueOf(attributes
				.get(CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME));
		Class<?>[] parameterTypes = (Class<?>[]) attributes
				.get(CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME);
		Method method;
		try {
			method = fallbacks.getMethod(methodName, parameterTypes);
			method.setAccessible(true);
		}
		catch (NoSuchMethodException e) {
			// TODO
			throw new RuntimeException(e);
		}
		circuitBreaker.run(() -> newRequest(requestValues).retrieve().toBodilessEntity(),
				throwable -> {
					try {
						return method.invoke(this,
								attributes.get(CircuitBreakerRequestValueProcessor.ARGUMENTS_ATTRIBUTE_NAME));
					}
					catch (IllegalAccessException | InvocationTargetException e) {
						// TODO
						throw new RuntimeException(e);
					}
				});
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		return circuitBreaker.run(() -> newRequest(values).retrieve().toBodilessEntity()
				.getHeaders());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return circuitBreaker.run(() -> {
			if (bodyType.getType().equals(InputStream.class)) {
				return (T) newRequest(values).exchange((request, response) -> response.getBody(), false);
			}
			return newRequest(values).retrieve().body(bodyType);
		});
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		return circuitBreaker.run(() -> newRequest(values).retrieve().toBodilessEntity());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return circuitBreaker.run(() -> {
			if (bodyType.getType().equals(InputStream.class)) {
				return (ResponseEntity<T>) newRequest(values).exchangeForRequiredValue((request, response) ->
						ResponseEntity.status(response.getStatusCode())
								.headers(response.getHeaders())
								.body(response.getBody()), false);
			}
			return newRequest(values).retrieve().toEntity(bodyType);
		});
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


	/**
	 * Create a {@link RestClientAdapter} for the given {@link RestClient}.
	 */
	public static CircuitBreakerRestClientAdapter create(RestClient restClient, CircuitBreaker circuitBreaker,
			Class<?> fallbacks) {
		return new CircuitBreakerRestClientAdapter(restClient, circuitBreaker, fallbacks);
	}

}
