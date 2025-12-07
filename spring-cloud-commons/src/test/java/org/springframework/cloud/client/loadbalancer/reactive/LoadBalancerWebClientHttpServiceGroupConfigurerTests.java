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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.cloud.client.loadbalancer.SimpleObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LoadBalancerWebClientHttpServiceGroupConfigurer}
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings({ "unchecked", "removal" })
class LoadBalancerWebClientHttpServiceGroupConfigurerTests {

	private static final String GROUP_NAME = "testService";

	private HttpServiceClientProperties clientServiceProperties;

	private ObjectProvider<DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction>> exchangeFilterFunctionProvider;

	@BeforeEach
	void setup() {
		DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction> exchangeFilterFunction = mock(
				DeferringLoadBalancerExchangeFilterFunction.class);
		exchangeFilterFunctionProvider = new SimpleObjectProvider<>(exchangeFilterFunction);
		clientServiceProperties = new HttpServiceClientProperties();
	}

	@Test
	void shouldAddInterceptorWhenBaseUrlIsNotSet() {
		LoadBalancerWebClientHttpServiceGroupConfigurer configurer = new LoadBalancerWebClientHttpServiceGroupConfigurer(
				exchangeFilterFunctionProvider, clientServiceProperties);
		TestGroups groups = new TestGroups();

		configurer.configureGroups(groups);

		groups.builder.filters(filterFunctions -> {
			assertThat(filterFunctions).hasSize(1);
			assertThat(filterFunctions.get(0).getClass()).isEqualTo(DeferringLoadBalancerExchangeFilterFunction.class);
		});
	}

	@Test
	void shouldAddInterceptorWhenBaseUrlHasLbScheme() {
		HttpClientProperties group = new HttpClientProperties();
		group.setBaseUrl("lb://" + GROUP_NAME + "/path");
		clientServiceProperties.put(GROUP_NAME, group);
		LoadBalancerWebClientHttpServiceGroupConfigurer configurer = new LoadBalancerWebClientHttpServiceGroupConfigurer(
				exchangeFilterFunctionProvider, clientServiceProperties);
		TestGroups groups = new TestGroups();

		configurer.configureGroups(groups);

		groups.builder.filters(filterFunctions -> {
			assertThat(filterFunctions).hasSize(1);
			assertThat(filterFunctions.get(0).getClass()).isEqualTo(DeferringLoadBalancerExchangeFilterFunction.class);
		});
	}

	@Test
	void shouldNotAddInterceptorWhenBaseDoesNotHaveLbScheme() {
		HttpClientProperties group = new HttpClientProperties();
		group.setBaseUrl("https://" + GROUP_NAME + "/path");
		clientServiceProperties.put(GROUP_NAME, group);
		LoadBalancerWebClientHttpServiceGroupConfigurer configurer = new LoadBalancerWebClientHttpServiceGroupConfigurer(
				exchangeFilterFunctionProvider, clientServiceProperties);
		TestGroups groups = new TestGroups();

		configurer.configureGroups(groups);

		groups.builder.filters(filterFunctions -> assertThat(filterFunctions).hasSize(0));
	}

	private static class TestGroups implements HttpServiceGroupConfigurer.Groups<WebClient.Builder> {

		WebClient.Builder builder = WebClient.builder();

		//
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
		public void forEachClient(HttpServiceGroupConfigurer.InitializingClientCallback<WebClient.Builder> callback) {

		}

		@Override
		public void forEachProxyFactory(HttpServiceGroupConfigurer.ProxyFactoryCallback configurer) {

		}

		@Override
		public void forEachGroup(HttpServiceGroupConfigurer.GroupCallback<WebClient.Builder> groupConfigurer) {
			groupConfigurer.withGroup(
					new TestGroup(GROUP_NAME, HttpServiceGroup.ClientType.WEB_CLIENT, new HashSet<>()), builder,
					HttpServiceProxyFactory.builder());
		}

	}

	private record TestGroup(String name, ClientType clientType,
			Set<Class<?>> httpServiceTypes) implements HttpServiceGroup {

	}

}
