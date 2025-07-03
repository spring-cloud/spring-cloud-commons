package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.ARGUMENTS_ATTRIBUTE_NAME;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME;

/**
 * @author Olga Maciaszek-Sharma
 */
class CircuitBreakerRestClientAdapterDecoratorTests {

	private final HttpExchangeAdapter adapter = mock(HttpExchangeAdapter.class);
	private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
	private final HttpRequestValues httpRequestValues = mock(HttpRequestValues.class);
	private final CircuitBreakerRestClientAdapterDecorator decorator = new CircuitBreakerRestClientAdapterDecorator(
			adapter, circuitBreaker, Fallbacks.class);


	@Test
	void shouldWrapAdapterCallsWithCircuitBreakerInvocation() {
		decorator.exchange(httpRequestValues);

		verify(circuitBreaker).run(any(), any());
	}

	@Test
	void shouldCreateFallbackHandler() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "test");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] {String.class, Integer.class});
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] {"testDescription", 5});
		when(httpRequestValues.getAttributes()).thenReturn(attributes);

		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test"));

		System.out.println("test");
	}

	@Test
	void shouldThrowExceptionWhenNoFallbackAvailable() {
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		assertThatExceptionOfType(NoFallbackAvailableException.class)
				.isThrownBy(() -> fallbackHandler.apply(new RuntimeException("test")));
	}

}