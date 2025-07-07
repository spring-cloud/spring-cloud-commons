/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.client.CloudHttpClientServiceProperties;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CircuitBreakerRestClientHttpServiceGroupConfigurer}.
 *
 * @author Olga Maciaszek-Sharma
 */
class CircuitBreakerRestClientHttpServiceGroupConfigurerTests {

	private static final String GROUP_NAME = "testService";

	private final CloudHttpClientServiceProperties clientServiceProperties = new CloudHttpClientServiceProperties();

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);

	private final TestGroups groups = new TestGroups();

	@BeforeEach
	void setUp() {
		when(circuitBreakerFactory.create(GROUP_NAME)).thenReturn(mock(CircuitBreaker.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldAddCircuitBreakerAdapterDecorator() {
		CloudHttpClientServiceProperties.Group group = new CloudHttpClientServiceProperties.Group();
		group.setFallbackClassName(Fallbacks.class.getCanonicalName());
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		CircuitBreakerRestClientHttpServiceGroupConfigurer configurer = new CircuitBreakerRestClientHttpServiceGroupConfigurer(
				clientServiceProperties, circuitBreakerFactory);
		ArgumentCaptor<Function<HttpExchangeAdapter, HttpExchangeAdapter>> captor = ArgumentCaptor
			.forClass(Function.class);

		configurer.configureGroups(groups);

		verify(groups.builder).exchangeAdapterDecorator(captor.capture());
		Function<HttpExchangeAdapter, HttpExchangeAdapter> captured = captor.getValue();
		CircuitBreakerAdapterDecorator decorator = (CircuitBreakerAdapterDecorator) captured
			.apply(new TestHttpExchangeAdapter());
		assertThat(decorator.getCircuitBreaker()).isNotNull();
		assertThat(decorator.getFallbackClass()).isAssignableFrom(Fallbacks.class);
	}

	@Test
	void shouldThrowExceptionWhenCantLoadClass() {
		CloudHttpClientServiceProperties.Group group = new CloudHttpClientServiceProperties.Group();
		group.setFallbackClassName("org.test.Fallback");
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		CircuitBreakerRestClientHttpServiceGroupConfigurer configurer = new CircuitBreakerRestClientHttpServiceGroupConfigurer(
				clientServiceProperties, circuitBreakerFactory);

		assertThatIllegalStateException().isThrownBy(() -> configurer.configureGroups(groups));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void shouldNotAddDecoratorWhenFallbackClassNameIsNull(String fallbackClassName) {
		CloudHttpClientServiceProperties.Group group = new CloudHttpClientServiceProperties.Group();
		group.setFallbackClassName(fallbackClassName);
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		CircuitBreakerRestClientHttpServiceGroupConfigurer configurer = new CircuitBreakerRestClientHttpServiceGroupConfigurer(
				clientServiceProperties, circuitBreakerFactory);

		assertThatNoException().isThrownBy(() -> configurer.configureGroups(groups));
		verify(circuitBreakerFactory, never()).create(GROUP_NAME);
	}

	private static class TestGroups implements HttpServiceGroupConfigurer.Groups<RestClient.Builder> {

		HttpServiceProxyFactory.Builder builder = mock(HttpServiceProxyFactory.Builder.class);

		@Override
		public HttpServiceGroupConfigurer.Groups<RestClient.Builder> filterByName(String... groupNames) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<RestClient.Builder> filter(Predicate<HttpServiceGroup> predicate) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public void forEachClient(HttpServiceGroupConfigurer.ClientCallback<RestClient.Builder> configurer) {

		}

		@Override
		public void forEachProxyFactory(HttpServiceGroupConfigurer.ProxyFactoryCallback configurer) {

		}

		@Override
		public void forEachGroup(HttpServiceGroupConfigurer.GroupCallback<RestClient.Builder> groupConfigurer) {
			groupConfigurer.withGroup(
					new TestGroup(GROUP_NAME, HttpServiceGroup.ClientType.REST_CLIENT, new HashSet<>()),
					RestClient.builder(), builder);
		}

	}

	private record TestGroup(String name, ClientType clientType,
			Set<Class<?>> httpServiceTypes) implements HttpServiceGroup {

	}

	private static class TestHttpExchangeAdapter implements HttpExchangeAdapter {

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

	}

}
