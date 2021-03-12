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

package org.springframework.cloud.loadbalancer.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerCacheAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Tests for {@link CachingServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = CachingServiceInstanceListSupplierTests.TestConfig.class)
@ExtendWith(SpringExtension.class)
class CachingServiceInstanceListSupplierTests {

	public static final String SERVICE_ID = "test";

	static {
		System.setProperty("loadbalancer.client.name", SERVICE_ID);
	}

	@Autowired
	BlockingLoadBalancerClient blockingLoadBalancerClient;

	private static DefaultServiceInstance instance(String host, boolean secure) {
		return new DefaultServiceInstance(SERVICE_ID, SERVICE_ID, host, 80, secure);
	}

	@Test
	void shouldNotHangOnCachingWhenDelegateReturnsInfiniteStream() {
		assertTimeoutPreemptively(ofMillis(1000), () -> {
			blockingLoadBalancerClient.choose(SERVICE_ID);
		});

	}

	@Configuration(proxyBeanMethods = false)
	@Import(LoadBalancerCacheAutoConfiguration.class)
	protected static class TestConfig {

		@Bean
		public ReactiveDiscoveryClient reactiveDiscoveryClient() {
			return new ReactiveDiscoveryClient() {
				@Override
				public String description() {
					return SERVICE_ID;
				}

				@Override
				public Flux<ServiceInstance> getInstances(String serviceId) {
					return Flux.just(instance("1host", false),
							instance("2host-secure", true));
				}

				@Override
				public Flux<String> getServices() {
					return Flux.just(SERVICE_ID);
				}
			};
		}

		@Bean
		ReactorLoadBalancer<ServiceInstance> reactorLoadBalancer(
				ObjectProvider<ServiceInstanceListSupplier> provider) {
			return new RoundRobinLoadBalancer(provider, SERVICE_ID);
		}

		@Bean
		LoadBalancerClientFactory loadBalancerClientFactory() {
			return new LoadBalancerClientFactory();
		}

		@Bean
		BlockingLoadBalancerClient blockingLoadBalancerClient(
				LoadBalancerClientFactory loadBalancerClientFactory) {
			return new BlockingLoadBalancerClient(loadBalancerClientFactory);
		}

		@Bean
		public LoadBalancerProperties loadBalancerProperties() {
			return new LoadBalancerProperties();
		}

		@Bean
		public WebClient.Builder webClientBuilder() {
			return WebClient.builder();
		}

		@Bean
		ServiceInstanceListSupplier supplier(ConfigurableApplicationContext context,
				ReactiveDiscoveryClient discoveryClient,
				LoadBalancerProperties loadBalancerProperties,
				WebClient.Builder webClientBuilder) {
			DiscoveryClientServiceInstanceListSupplier firstDelegate = new DiscoveryClientServiceInstanceListSupplier(
					discoveryClient, context.getEnvironment());
			HealthCheckServiceInstanceListSupplier delegate = new TestHealthCheckServiceInstanceListSupplier(
					firstDelegate, loadBalancerProperties.getHealthCheck(),
					webClientBuilder.build());
			delegate.afterPropertiesSet();
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			return new CachingServiceInstanceListSupplier(delegate,
					cacheManagerProvider.getIfAvailable());
		}

		private static class TestHealthCheckServiceInstanceListSupplier
				extends HealthCheckServiceInstanceListSupplier {

			TestHealthCheckServiceInstanceListSupplier(
					ServiceInstanceListSupplier delegate,
					LoadBalancerProperties.HealthCheck healthCheck, WebClient webClient) {
				super(delegate, healthCheck, webClient);
			}

			@Override
			protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
				return Mono.just(true);
			}

		}

	}

}
