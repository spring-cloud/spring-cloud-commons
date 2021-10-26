/*
 * Copyright 2012-2020 the original author or authors.
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

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerTestUtils.assertLoadBalanced;
import static org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerTestUtils.getFilters;

/**
 * Tests for {@link ReactorLoadBalancerClientAutoConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 */
public class ReactorLoadBalancerClientAutoConfigurationTests {

	@Test
	void loadBalancerFilterAddedToWebClientBuilder() {
		ConfigurableApplicationContext context = init(OneWebClientBuilder.class);
		final Map<String, WebClient.Builder> webClientBuilders = context.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).isNotNull().hasSize(1);
		WebClient.Builder webClientBuilder = webClientBuilders.values().iterator().next();
		then(webClientBuilder).isNotNull();

		assertLoadBalanced(webClientBuilder, ReactorLoadBalancerExchangeFilterFunction.class);

		final Map<String, OneWebClientBuilder.TestService> testServiceMap = context
				.getBeansOfType(OneWebClientBuilder.TestService.class);
		then(testServiceMap).isNotNull().hasSize(1);
		OneWebClientBuilder.TestService testService = testServiceMap.values().stream().findFirst().get();
		assertLoadBalanced(testService.webClient, ReactorLoadBalancerExchangeFilterFunction.class);
	}

	@Test
	void loadBalancerFilterAddedToWebClientBuilderWithRetryEnabled() {
		System.setProperty("spring.cloud.loadbalancer.retry.enabled", "true");
		ConfigurableApplicationContext context = init(OneWebClientBuilder.class);
		final Map<String, WebClient.Builder> webClientBuilders = context.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).isNotNull().hasSize(1);
		WebClient.Builder webClientBuilder = webClientBuilders.values().iterator().next();
		then(webClientBuilder).isNotNull();

		assertLoadBalanced(webClientBuilder, RetryableLoadBalancerExchangeFilterFunction.class);

		final Map<String, OneWebClientBuilder.TestService> testServiceMap = context
				.getBeansOfType(OneWebClientBuilder.TestService.class);
		then(testServiceMap).isNotNull().hasSize(1);
		OneWebClientBuilder.TestService testService = testServiceMap.values().stream().findFirst().get();
		assertLoadBalanced(testService.webClient, RetryableLoadBalancerExchangeFilterFunction.class);

		System.clearProperty("spring.cloud.loadbalancer.retry.enabled");
	}

	@Test
	void loadBalancerFilterAddedOnlyToLoadBalancedWebClientBuilder() {
		ConfigurableApplicationContext context = init(TwoWebClientBuilders.class);
		final Map<String, WebClient.Builder> webClientBuilders = context.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(2);

		TwoWebClientBuilders.Two two = context.getBean(TwoWebClientBuilders.Two.class);

		then(two.loadBalanced).isNotNull();
		assertLoadBalanced(two.loadBalanced, ReactorLoadBalancerExchangeFilterFunction.class);

		then(two.nonLoadBalanced).isNotNull();
		then(getFilters(two.nonLoadBalanced)).isNullOrEmpty();
	}

	@Test
	void loadBalancerFilterAddedOnlyToLoadBalancedWebClientBuilderWithRetryEnabled() {
		System.setProperty("spring.cloud.loadbalancer.retry.enabled", "true");
		ConfigurableApplicationContext context = init(TwoWebClientBuilders.class);
		final Map<String, WebClient.Builder> webClientBuilders = context.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(2);

		TwoWebClientBuilders.Two two = context.getBean(TwoWebClientBuilders.Two.class);

		then(two.loadBalanced).isNotNull();
		assertLoadBalanced(two.loadBalanced, RetryableLoadBalancerExchangeFilterFunction.class);

		then(two.nonLoadBalanced).isNotNull();
		then(getFilters(two.nonLoadBalanced)).isNullOrEmpty();
		System.clearProperty("spring.cloud.loadbalancer.retry.enabled");
	}

	@Test
	void noCustomWebClientBuilders() {
		ConfigurableApplicationContext context = init(NoWebClientBuilder.class);
		final Map<String, WebClient.Builder> webClientBuilders = context.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(1);

		WebClient.Builder builder = context.getBean(WebClient.Builder.class);

		then(builder).isNotNull();
		then(getFilters(builder)).isNullOrEmpty();
	}

	@Test
	void defaultPropertiesWorks() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(OneWebClientBuilder.class, DefaulConfig.class)
				.properties("spring.cloud.loadbalancer.health-check.initial-delay=1s",
						"spring.cloud.loadbalancer.clients.myclient.health-check.interval=30s")
				.run();
		LoadBalancerClientsProperties properties = context.getBean(LoadBalancerClientsProperties.class);

		then(properties.getClients()).containsKey("myclient");
		LoadBalancerProperties clientProperties = properties.getClients().get("myclient");
		// default value
		then(clientProperties.getHealthCheck().getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
		// client specific value
		then(clientProperties.getHealthCheck().getInterval()).isEqualTo(Duration.ofSeconds(30));
	}

	private ConfigurableApplicationContext init(Class<?> config) {
		return LoadBalancerTestUtils.init(config, ReactorLoadBalancerClientAutoConfiguration.class,
				LoadBalancerBeanPostProcessorAutoConfiguration.class);
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class DefaulConfig {

	}

	@Configuration
	protected static class NoWebClientBuilder {

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory(LoadBalancerProperties properties) {
			return new ReactiveLoadBalancer.Factory<ServiceInstance>() {
				@Override
				public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
					return new TestReactiveLoadBalancer();
				}

				@Override
				public <X> Map<String, X> getInstances(String name, Class<X> type) {
					throw new UnsupportedOperationException("Not implemented");
				}

				@Override
				public <X> X getInstance(String name, Class<?> clazz, Class<?>... generics) {
					throw new UnsupportedOperationException("Not implemented.");
				}

				@Override
				public LoadBalancerProperties getProperties(String serviceId) {
					return properties;
				}
			};
		}

		@Bean
		LoadBalancedRetryFactory loadBalancedRetryFactory() {
			return new LoadBalancedRetryFactory() {
			};
		}

	}

	@Configuration
	protected static class OneWebClientBuilder extends NoWebClientBuilder {

		@Bean
		@LoadBalanced
		WebClient.Builder loadBalancedWebClientBuilder() {
			return WebClient.builder();
		}

		@Bean
		TestService testService() {
			return new TestService(loadBalancedWebClientBuilder());
		}

		private final class TestService {

			public final WebClient webClient;

			private TestService(WebClient.Builder builder) {
				this.webClient = builder.build();
			}

		}

	}

	@Configuration
	protected static class TwoWebClientBuilders extends OneWebClientBuilder {

		@Primary
		@Bean
		WebClient.Builder webClientBuilder() {
			return WebClient.builder();
		}

		@Configuration
		protected static class Two {

			@Autowired
			WebClient.Builder nonLoadBalanced;

			@Autowired
			@LoadBalanced
			WebClient.Builder loadBalanced;

		}

	}

}
