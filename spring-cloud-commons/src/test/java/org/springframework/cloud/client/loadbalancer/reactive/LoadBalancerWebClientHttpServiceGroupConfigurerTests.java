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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.client.reactive.service.ReactiveHttpClientServiceProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.SimpleObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LoadBalancerWebClientHttpServiceGroupConfigurer}
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings({"unchecked", "removal"})
class LoadBalancerWebClientHttpServiceGroupConfigurerTests {

	private static final String GROUP_NAME = "testService";
	private ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory;
	private ReactiveHttpClientServiceProperties clientServiceProperties;
	private ObjectProvider<DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction>> exchangeFilterFunctionProvider;

	@BeforeEach
	void setup() {
		DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction> exchangeFilterFunction =
				mock(DeferringLoadBalancerExchangeFilterFunction.class);
		exchangeFilterFunctionProvider = new SimpleObjectProvider<>(exchangeFilterFunction);
		loadBalancerClientFactory = mock(ReactiveLoadBalancer.Factory.class);
		clientServiceProperties = new ReactiveHttpClientServiceProperties();
		LoadBalancerProperties properties = new LoadBalancerProperties();
		when(loadBalancerClientFactory.getProperties(GROUP_NAME)).thenReturn(properties);
	}

	@Test
	void shouldAddInterceptorWhenBaseUrlIsNotSet() {
		LoadBalancerWebClientHttpServiceGroupConfigurer configurer = new LoadBalancerWebClientHttpServiceGroupConfigurer(exchangeFilterFunctionProvider,
				clientServiceProperties, loadBalancerClientFactory);
		TestGroups groups = new TestGroups();

		configurer.configureGroups(groups);

		groups.builder.filters(filterFunctions -> {
					assertThat(filterFunctions).hasSize(1);
					assertThat(filterFunctions.get(0)
							.getClass()).isEqualTo(DeferringLoadBalancerExchangeFilterFunction.class);
				}
		);
	}

//		properties.getInterfaceClients().setDefaultScheme("https");

	@Test
	void shouldAddInterceptorWhenBaseUrlIsServiceIdUrl() {
		ReactiveHttpClientServiceProperties.Group group = new ReactiveHttpClientServiceProperties.Group();
		group.setBaseUrl("https://" + GROUP_NAME + "/path");
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		LoadBalancerWebClientHttpServiceGroupConfigurer configurer = new LoadBalancerWebClientHttpServiceGroupConfigurer(exchangeFilterFunctionProvider,
				clientServiceProperties, loadBalancerClientFactory);
		TestGroups groups = new TestGroups();

		configurer.configureGroups(groups);

		groups.builder.filters(filterFunctions -> {
					assertThat(filterFunctions).hasSize(1);
					assertThat(filterFunctions.get(0)
							.getClass()).isEqualTo(DeferringLoadBalancerExchangeFilterFunction.class);
				}
		);
	}

	@Test
	void shouldNotAddInterceptorWhenBaseUrlIsNotServiceIdUrl() {
		ReactiveHttpClientServiceProperties.Group group = new ReactiveHttpClientServiceProperties.Group();
		group.setBaseUrl("https://some-other-service/path");
		clientServiceProperties.getGroup().put(GROUP_NAME, group);
		LoadBalancerWebClientHttpServiceGroupConfigurer configurer =
				new LoadBalancerWebClientHttpServiceGroupConfigurer(exchangeFilterFunctionProvider,
						clientServiceProperties, loadBalancerClientFactory);
		TestGroups groups = new TestGroups();

		configurer.configureGroups(groups);

		groups.builder.filters(
				filterFunctions -> assertThat(filterFunctions).hasSize(0)
		);
	}


	private static class TestGroups implements HttpServiceGroupConfigurer.Groups<WebClient.Builder> {

		WebClient.Builder builder = WebClient.builder();


		@Override
		public HttpServiceGroupConfigurer.Groups<WebClient.Builder> filterByName(String... groupNames) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<WebClient.Builder> filter(Predicate<HttpServiceGroup> predicate) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public void configureClient(Consumer<WebClient.Builder> clientConfigurer) {

		}

		@Override
		public void configureClient(BiConsumer<HttpServiceGroup, WebClient.Builder> clientConfigurer) {
			clientConfigurer.accept(new TestGroup(GROUP_NAME, HttpServiceGroup.ClientType.WEB_CLIENT, new HashSet<>()), builder);
		}

		@Override
		public void configureProxyFactory(BiConsumer<HttpServiceGroup, HttpServiceProxyFactory.Builder> proxyFactoryConfigurer) {

		}

		@Override
		public void configure(BiConsumer<HttpServiceGroup, WebClient.Builder> clientConfigurer, BiConsumer<HttpServiceGroup, HttpServiceProxyFactory.Builder> proxyFactoryConfigurer) {

		}
	}

	private record TestGroup(String name, ClientType clientType,
							 Set<Class<?>> httpServiceTypes)
			implements HttpServiceGroup {

	}
}
