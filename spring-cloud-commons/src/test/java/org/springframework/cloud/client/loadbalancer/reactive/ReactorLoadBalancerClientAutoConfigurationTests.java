/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;
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
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).isNotNull().hasSize(1);
		WebClient.Builder webClientBuilder = webClientBuilders.values().iterator().next();
		then(webClientBuilder).isNotNull();

		assertLoadBalanced(webClientBuilder);
	}

	@Test
	void loadBalancerFilterAddedOnlyToLoadBalancedWebClientBuilder() {
		ConfigurableApplicationContext context = init(TwoWebClientBuilders.class);
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(2);

		TwoWebClientBuilders.Two two = context.getBean(TwoWebClientBuilders.Two.class);

		then(two.loadBalanced).isNotNull();
		assertLoadBalanced(two.loadBalanced);

		then(two.nonLoadBalanced).isNotNull();
		then(getFilters(two.nonLoadBalanced)).isNullOrEmpty();
	}

	@Test
	void noCustomWebClientBuilders() {
		ConfigurableApplicationContext context = init(NoWebClientBuilder.class);
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(1);

		WebClient.Builder builder = context.getBean(WebClient.Builder.class);

		then(builder).isNotNull();
		then(getFilters(builder)).isNullOrEmpty();
	}

	private ConfigurableApplicationContext init(Class<?> config) {
		return LoadBalancerTestUtils.init(config,
				ReactorLoadBalancerClientAutoConfiguration.class);
	}

	private void assertLoadBalanced(WebClient.Builder webClientBuilder) {
		LoadBalancerTestUtils.assertLoadBalanced(webClientBuilder,
				ReactorLoadBalancerExchangeFilterFunction.class);
	}

	@Configuration
	protected static class NoWebClientBuilder {

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory() {
			return serviceId -> new TestReactiveLoadBalancer();
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
