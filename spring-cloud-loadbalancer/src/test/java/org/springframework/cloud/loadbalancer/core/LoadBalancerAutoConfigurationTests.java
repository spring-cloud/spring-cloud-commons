/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Nikola Kološnjaji
 */
public class LoadBalancerAutoConfigurationTests {

	@Test
	public void noCustomWebClientBuilders() {
		ConfigurableApplicationContext context = init(NoWebClientBuilder.class);
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(1);

		WebClient.Builder builder = context.getBean(WebClient.Builder.class);

		then(builder).isNotNull();
		then(getFilters(builder)).isNullOrEmpty();
	}

	@Test
	public void webClientBuilderGetsLoadBalancerInterceptor() {
		ConfigurableApplicationContext context = init(OneWebClientBuilder.class);
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).isNotNull().hasSize(1);
		WebClient.Builder webClientBuilder = webClientBuilders.values().iterator().next();
		then(webClientBuilder).isNotNull();

		assertLoadBalanced(webClientBuilder);
	}

	@Test
	public void multipleWebClientBuilders() {
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

	private void assertLoadBalanced(WebClient.Builder webClientBuilder) {
		List<ExchangeFilterFunction> filters = getFilters(webClientBuilder);
		then(filters).hasSize(1);
		ExchangeFilterFunction interceptor = filters.get(0);
		then(interceptor).isInstanceOf(ReactiveLoadBalancerExchangeFilterFunction.class);
	}

	@SuppressWarnings("unchecked")
	private List<ExchangeFilterFunction> getFilters(WebClient.Builder builder) {
		return (List<ExchangeFilterFunction>) ReflectionTestUtils.getField(builder,
				"filters");
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(config,
				WebClientAutoConfiguration.class, LoadBalancerAutoConfiguration.class)
				.run();
	}

	@Configuration
	protected static class NoWebClientBuilder {

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
