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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.http.HttpHeaders;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
 * Tests for {@link ReactiveCircuitBreakerAdapterDecorator}.
 *
 * @author Olga Maciaszek-Sharma
 */
class ReactiveCircuitBreakerAdapterDecoratorTests {

	private static final String TEST_DESCRIPTION = "testDescription";

	private static final int TEST_VALUE = 5;

	private final ReactorHttpExchangeAdapter adapter = mock(ReactorHttpExchangeAdapter.class);

	private final ReactiveCircuitBreaker reactiveCircuitBreaker = mock(ReactiveCircuitBreaker.class);

	private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

	private final HttpRequestValues httpRequestValues = mock(HttpRequestValues.class);

	// Also verifies class fallback won't override default fallback for other classes
	private final ReactiveCircuitBreakerAdapterDecorator decorator = new ReactiveCircuitBreakerAdapterDecorator(adapter,
			reactiveCircuitBreaker, circuitBreaker, Map.of(DEFAULT_FALLBACK_KEY, Fallbacks.class,
					UnusedTestService.class.getCanonicalName(), EmptyFallbacks.class));

	@BeforeEach
	void setUp() {
		when(adapter.exchangeForBodyMono(any(), any())).thenReturn(Mono.just("test"));
	}

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
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test"));

		assertThat(fallback).isEqualTo(TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@Test
	void shouldCreateBodyMonoFallbackHandler() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testMono");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Mono.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Mono<Object>> fallbackHandler = decorator.createBodyMonoFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).block();

		assertThat(fallback).isEqualTo(TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@Test
	void shouldCreateBodyMonoFallbackHandlerFromPerClassFallbackClassNames() {
		Map<String, Class<?>> perClassFallbackClassNames = Map.of(DEFAULT_FALLBACK_KEY, EmptyFallbacks.class,
				TestService.class.getCanonicalName(), Fallbacks.class);
		ReactiveCircuitBreakerAdapterDecorator decorator = new ReactiveCircuitBreakerAdapterDecorator(adapter,
				reactiveCircuitBreaker, circuitBreaker, perClassFallbackClassNames);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testMono");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Mono.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Mono<Object>> fallbackHandler = decorator.createBodyMonoFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).block();

		assertThat(fallback).isEqualTo(TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@Test
	void shouldCreateBodyMonoFallbackHandlerForNonReactiveReturnType() {
		assertThatCode(() -> {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put(METHOD_ATTRIBUTE_NAME, "test");
			attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
			attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
			attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
			attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
			when(httpRequestValues.getAttributes()).thenReturn(attributes);
			Function<Throwable, Mono<Object>> fallbackHandler = decorator
				.createBodyMonoFallbackHandler(httpRequestValues);

			fallbackHandler.apply(new RuntimeException("test")).block();
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldCreateBodyFluxFallbackHandlerForNonReactiveReturnType() {
		assertThatCode(() -> {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put(METHOD_ATTRIBUTE_NAME, "test");
			attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
			attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
			attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
			attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
			when(httpRequestValues.getAttributes()).thenReturn(attributes);
			Function<Throwable, Flux<Object>> fallbackHandler = decorator
				.createBodyFluxFallbackHandler(httpRequestValues);

			fallbackHandler.apply(new RuntimeException("test")).blockFirst();
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldCreateBodyMonoFallbackHandlerForVoidReturnType() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "post");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Void.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Mono<Object>> fallbackHandler = decorator.createBodyMonoFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).block();

		assertThat(fallback).isNull();
	}

	@Test
	void shouldCreateBodyFluxFallbackHandler() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testFlux");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Flux.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Flux<Object>> fallbackHandler = decorator.createBodyFluxFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).blockFirst();

		assertThat(fallback).isEqualTo(TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@Test
	void shouldCreateBodyFluxFallbackHandlerFromNonReactiveReturnType() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "test");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Flux<Object>> fallbackHandler = decorator.createBodyFluxFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).blockFirst();

		assertThat(fallback).isEqualTo(TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@Test
	void shouldCreateBodyFluxFallbackHandlerFromReactiveReturnType() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testFlux");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Flux.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Flux<Object>> fallbackHandler = decorator.createBodyFluxFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).blockFirst();

		assertThat(fallback).isEqualTo(TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void shouldCreateHttpHeadersMonoFallbackHandler() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testHttpHeadersMono");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Mono.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Mono<HttpHeaders>> fallbackHandler = decorator
			.createHttpHeadersMonoFallbackHandler(httpRequestValues);

		HttpHeaders fallback = fallbackHandler.apply(new RuntimeException("test")).block();

		assertThat(fallback.get(TEST_DESCRIPTION).get(0)).isEqualTo(String.valueOf(TEST_VALUE));
	}

	@Test
	void shouldCreateFallbackHandlerWithCause() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testThrowable");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { Throwable.class, String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { new Throwable("test!"), TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, String.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Object> fallbackHandler = decorator.createFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test"));

		assertThat(fallback).isEqualTo("java.lang.Throwable: test! " + TEST_DESCRIPTION + ": " + TEST_VALUE);
	}

	@Test
	void shouldCreateReactiveFallbackHandlerWithCause() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_ATTRIBUTE_NAME, "testThrowableMono");
		attributes.put(PARAMETER_TYPES_ATTRIBUTE_NAME, new Class<?>[] { Throwable.class, String.class, Integer.class });
		attributes.put(ARGUMENTS_ATTRIBUTE_NAME, new Object[] { new Throwable("test!"), TEST_DESCRIPTION, TEST_VALUE });
		attributes.put(RETURN_TYPE_ATTRIBUTE_NAME, Mono.class);
		attributes.put(DECLARING_CLASS_ATTRIBUTE_NAME, TestService.class.getCanonicalName());
		when(httpRequestValues.getAttributes()).thenReturn(attributes);
		Function<Throwable, Mono<Object>> fallbackHandler = decorator.createBodyMonoFallbackHandler(httpRequestValues);

		Object fallback = fallbackHandler.apply(new RuntimeException("test")).block();

		assertThat(fallback).isEqualTo("java.lang.Throwable: test! " + TEST_DESCRIPTION + ": " + TEST_VALUE);
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
