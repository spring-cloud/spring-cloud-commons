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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public class ReactiveLoadBalancerAutoConfigurationTests {

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

	private void assertLoadBalanced(WebClient.Builder webClientBuilder) {
		List<ExchangeFilterFunction> filters = getFilters(webClientBuilder);
		then(filters).hasSize(1);
		ExchangeFilterFunction interceptor = filters.get(0);
		then(interceptor).isInstanceOf(LoadBalancerExchangeFilterFunction.class);
	}

	@SuppressWarnings("unchecked")
	private List<ExchangeFilterFunction> getFilters(WebClient.Builder builder) {
		return (List<ExchangeFilterFunction>) ReflectionTestUtils.getField(builder,
				"filters");
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

	protected ConfigurableApplicationContext init(Class<?> config) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE)
				// .properties("spring.aop.proxyTargetClass=true")
				.sources(config, WebClientAutoConfiguration.class,
						ReactiveLoadBalancerAutoConfiguration.class)
				.run();
	}

	@Configuration
	protected static class NoWebClientBuilder {

		@Bean
		LoadBalancerClient loadBalancerClient() {
			return new NoopLoadBalancerClient();
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

	private static class NoopLoadBalancerClient implements LoadBalancerClient {

		private final Random random = new Random();

		@Override
		public ServiceInstance choose(String serviceId) {
			return new DefaultServiceInstance(serviceId, serviceId, serviceId,
					this.random.nextInt(40000), false);
		}

		@Override
		public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
			try {
				return request.apply(choose(serviceId));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public <T> T execute(String serviceId, ServiceInstance serviceInstance,
				LoadBalancerRequest<T> request) throws IOException {
			try {
				return request.apply(choose(serviceId));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public URI reconstructURI(ServiceInstance instance, URI original) {
			return DefaultServiceInstance.getUri(instance);
		}

	}

}
