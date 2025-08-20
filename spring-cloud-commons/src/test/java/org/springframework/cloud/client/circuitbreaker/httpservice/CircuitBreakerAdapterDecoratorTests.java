/*
 * Copyright 2012-present the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.DEFAULT_FALLBACK_KEY;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.ARGUMENTS_ATTRIBUTE_NAME;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.DECLARING_CLASS_ATTRIBUTE_NAME;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.RETURN_TYPE_ATTRIBUTE_NAME;

/**
 * Tests for {@link CircuitBreakerAdapterDecorator}.
 *
 * @author Olga Maciaszek-Sharma
 */
class CircuitBreakerAdapterDecoratorTests {

	private final HttpExchangeAdapter adapter = mock(HttpExchangeAdapter.class);

	private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

	private final HttpRequestValues httpRequestValues = mock(HttpRequestValues.class);

	// Also verifies class fallback won't override default fallback for other classes
	private final CircuitBreakerAdapterDecorator decorator = new CircuitBreakerAdapterDecorator(adapter, circuitBreaker,
			Map.of(DEFAULT_FALLBACK_KEY, Fallbacks.class, UnusedTestService.class.getCanonicalName(),
					EmptyFallbacks.class));

	@Test
	void shouldWrapAdapterCallsWithCircuitBreakerInvocation() {
		decorator.exchange(httpRequestValues);

		verify(circuitBreaker).run(any(), any());
	}

	@Test
	void shouldCreateFallbackHandler() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "test");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { "testDescription", 5 });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test"));

		assertThat(fallback).isEqualTo("testDescription: 5");
	}

	@Test
	void shouldCreateFallbackHandlerFromPerClassFallbackClassNames() {
		Map<String, Class<?>> perClassFallbackClassNames = Map.of(DEFAULT_FALLBACK_KEY, EmptyFallbacks.class,
				TestService.class.getCanonicalName(), Fallbacks.class);
		CircuitBreakerAdapterDecorator decorator = new CircuitBreakerAdapterDecorator(adapter, circuitBreaker,
				perClassFallbackClassNames);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "test");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { "testDescription", 5 });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test"));

		assertThat(fallback).isEqualTo("testDescription: 5");
	}

	@Test
	void shouldCreateFallbackHandlerWithCause() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testThrowable");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { Throwable.class, String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { new Throwable("test!"), "testDescription", 5 });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test"));

		assertThat(fallback).isEqualTo("java.lang.Throwable: test! testDescription: 5");
	}

	@Test
	void shouldThrowExceptionWhenNoFallbackAvailable() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		assertThatExceptionOfType(NoFallbackAvailableException.class)
			.isThrownBy(() -> fallbackHandler.apply(new RuntimeException("test")));
	}

}
