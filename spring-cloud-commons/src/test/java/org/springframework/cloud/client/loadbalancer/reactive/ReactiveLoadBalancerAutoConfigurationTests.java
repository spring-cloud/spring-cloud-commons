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

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerTestUtils.assertLoadBalanced;
import static org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerTestUtils.getFilters;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
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

		assertLoadBalanced(webClientBuilder, LoadBalancerExchangeFilterFunction.class);

		final Map<String, OneWebClientBuilder.TestService> testServiceMap = context
				.getBeansOfType(OneWebClientBuilder.TestService.class);
		then(testServiceMap).isNotNull().hasSize(1);
		OneWebClientBuilder.TestService testService = testServiceMap.values().stream()
				.findFirst().get();
		assertLoadBalanced(testService.webClient,
				LoadBalancerExchangeFilterFunction.class);
	}

	@Test
	public void multipleWebClientBuilders() {
		ConfigurableApplicationContext context = init(TwoWebClientBuilders.class);
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(2);

		TwoWebClientBuilders.Two two = context.getBean(TwoWebClientBuilders.Two.class);

		then(two.loadBalanced).isNotNull();
		assertLoadBalanced(two.loadBalanced, LoadBalancerExchangeFilterFunction.class);

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

	@Test
	public void autoConfigurationNotLoadedWhenReactorLoadBalancerExchangeFilterFunctionPresent() {
		ConfigurableApplicationContext context = init(
				ReactorLoadBalancerClientPresent.class);
		final Map<String, WebClient.Builder> webClientBuilders = context
				.getBeansOfType(WebClient.Builder.class);

		then(webClientBuilders).hasSize(1);

		assertThatThrownBy(
				() -> context.getBean(LoadBalancerExchangeFilterFunction.class))
						.isInstanceOf(NoSuchBeanDefinitionException.class);

		WebClient.Builder builder = context.getBean(WebClient.Builder.class);

		then(builder).isNotNull();
		assertLoadBalanced(builder, ReactorLoadBalancerExchangeFilterFunction.class);
	}

	private ConfigurableApplicationContext init(Class<?> config) {
		return LoadBalancerTestUtils.init(config,
				ReactiveLoadBalancerAutoConfiguration.class,
				LoadBalancerBeanPostProcessorAutoConfiguration.class);
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
	protected static class ReactorLoadBalancerClientPresent extends OneWebClientBuilder {

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory() {
			return serviceId -> new TestReactiveLoadBalancer();
		}

		@Bean
		ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction() {
			return new ReactorLoadBalancerExchangeFilterFunction(
					reactiveLoadBalancerFactory());
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
