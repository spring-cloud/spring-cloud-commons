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

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.CloudHttpClientServiceProperties;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapter;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.DEFAULT_FALLBACK_KEY;

/**
 * Tests for {@link CircuitBreakerWebClientHttpServiceGroupConfigurer}.
 *
 * @author Olga Maciaszek-Sharma
 */
class CircuitBreakerWebClientHttpServiceGroupConfigurerTests {

	private static final String GROUP_NAME = "testService";

	private final CloudHttpClientServiceProperties clientServiceProperties = new CloudHttpClientServiceProperties();

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);

	private final ReactiveCircuitBreakerFactory<?, ?> reactiveCircuitBreakerFactory = mock(
			ReactiveCircuitBreakerFactory.class);

	private final TestGroups groups = new TestGroups();

	@BeforeEach
	void setUp() {
		when(circuitBreakerFactory.create(GROUP_NAME)).thenReturn(mock(CircuitBreaker.class));
		when(reactiveCircuitBreakerFactory.create(GROUP_NAME + "-reactive"))
			.thenReturn(mock(ReactiveCircuitBreaker.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldAddCircuitBreakerAdapterDecorator() {
		CloudHttpClientServiceProperties.Group group = new CloudHttpClientServiceProperties.Group();
		group.setFallbackClassNames(Collections.singletonMap(DEFAULT_FALLBACK_KEY, Fallbacks.class.getCanonicalName()));
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		CircuitBreakerWebClientHttpServiceGroupConfigurer configurer = new CircuitBreakerWebClientHttpServiceGroupConfigurer(
				clientServiceProperties, reactiveCircuitBreakerFactory, circuitBreakerFactory);
		ArgumentCaptor<Function<HttpExchangeAdapter, HttpExchangeAdapter>> captor = ArgumentCaptor
			.forClass(Function.class);

		configurer.configureGroups(groups);

		verify(groups.builder).exchangeAdapterDecorator(captor.capture());
		Function<HttpExchangeAdapter, HttpExchangeAdapter> captured = captor.getValue();
		ReactiveCircuitBreakerAdapterDecorator decorator = (ReactiveCircuitBreakerAdapterDecorator) captured
			.apply(new TestHttpExchangeAdapter());
		assertThat(decorator.getCircuitBreaker()).isNotNull();
		assertThat(decorator.getReactiveCircuitBreaker()).isNotNull();
		assertThat(decorator.getFallbackClasses().get(DEFAULT_FALLBACK_KEY)).isAssignableFrom(Fallbacks.class);
	}

	@Test
	void shouldThrowExceptionWhenCantLoadClass() {
		CloudHttpClientServiceProperties.Group group = new CloudHttpClientServiceProperties.Group();
		group.setFallbackClassNames(Collections.singletonMap(DEFAULT_FALLBACK_KEY, "org.test.Fallback"));
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		CircuitBreakerWebClientHttpServiceGroupConfigurer configurer = new CircuitBreakerWebClientHttpServiceGroupConfigurer(
				clientServiceProperties, reactiveCircuitBreakerFactory, circuitBreakerFactory);

		assertThatIllegalStateException().isThrownBy(() -> configurer.configureGroups(groups));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void shouldNotAddDecoratorWhenFallbackClassNamesNullOrEmpty(Map<String, String> fallbackClassNames) {
		CloudHttpClientServiceProperties.Group group = new CloudHttpClientServiceProperties.Group();
		group.setFallbackClassNames(fallbackClassNames);
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		CircuitBreakerWebClientHttpServiceGroupConfigurer configurer = new CircuitBreakerWebClientHttpServiceGroupConfigurer(
				clientServiceProperties, reactiveCircuitBreakerFactory, circuitBreakerFactory);

		assertThatNoException().isThrownBy(() -> configurer.configureGroups(groups));
		verify(circuitBreakerFactory, never()).create(GROUP_NAME);
	}

	private static class TestGroups implements HttpServiceGroupConfigurer.Groups<WebClient.Builder> {

		HttpServiceProxyFactory.Builder builder = mock(HttpServiceProxyFactory.Builder.class);

		@Override
		public HttpServiceGroupConfigurer.Groups<WebClient.Builder> filterByName(String... groupNames) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<WebClient.Builder> filter(Predicate<HttpServiceGroup> predicate) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public void forEachClient(HttpServiceGroupConfigurer.ClientCallback<WebClient.Builder> configurer) {

		}

		@Override
		public void forEachProxyFactory(HttpServiceGroupConfigurer.ProxyFactoryCallback configurer) {

		}

		@Override
		public void forEachGroup(HttpServiceGroupConfigurer.GroupCallback<WebClient.Builder> groupConfigurer) {
			groupConfigurer.withGroup(
					new TestGroup(GROUP_NAME, HttpServiceGroup.ClientType.REST_CLIENT, new HashSet<>()),
					WebClient.builder(), builder);
		}

	}

	private record TestGroup(String name, ClientType clientType,
			Set<Class<?>> httpServiceTypes) implements HttpServiceGroup {

	}

	private static class TestHttpExchangeAdapter implements ReactorHttpExchangeAdapter {

		@Override
		public boolean supportsRequestAttributes() {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public void exchange(HttpRequestValues requestValues) {

		}

		@Override
		public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public <T> @Nullable T exchangeForBody(HttpRequestValues requestValues,
				ParameterizedTypeReference<T> bodyType) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues,
				ParameterizedTypeReference<T> bodyType) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public @Nullable Duration getBlockTimeout() {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues,
				ParameterizedTypeReference<T> bodyType) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues,
				ParameterizedTypeReference<T> bodyType) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues requestValues,
				ParameterizedTypeReference<T> bodyType) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues requestValues,
				ParameterizedTypeReference<T> bodyType) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

	}

}
