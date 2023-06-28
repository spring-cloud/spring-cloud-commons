/*
 * Copyright 2013-2023 the original author or authors.
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

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ServiceInstanceListSupplierBuilderTests {

	@Test
	public void testBuilder() {
		new ApplicationContextRunner().withUserConfiguration(CacheTestConfig.class).run(context -> {
			ServiceInstanceListSupplier supplier = ServiceInstanceListSupplier.builder().withDiscoveryClient()
					.withHealthChecks().build(context);
			assertThat(supplier).isInstanceOf(HealthCheckServiceInstanceListSupplier.class);
			DelegatingServiceInstanceListSupplier delegating = (DelegatingServiceInstanceListSupplier) supplier;
			assertThat(delegating.getDelegate()).isInstanceOf(DiscoveryClientServiceInstanceListSupplier.class);
		});
	}

	@Test
	public void testIllegalArgumentExceptionThrownWhenBaseBuilderNull() {
		new ApplicationContextRunner().withUserConfiguration(CacheTestConfig.class).run(context -> {
			try {
				ServiceInstanceListSupplier.builder().withHealthChecks().build(context);
				fail("Should have thrown exception.");
			}
			catch (Exception exception) {
				assertThat(exception).isInstanceOf(IllegalArgumentException.class);
			}

		});
	}

	@Test
	public void testDelegateReturnedIfLoadBalancerCacheManagerNotAvailable() {
		new ApplicationContextRunner().withUserConfiguration(BaseTestConfig.class).run(context -> {
			ServiceInstanceListSupplier supplier = ServiceInstanceListSupplier.builder().withDiscoveryClient()
					.withHealthChecks().withCaching().build(context);
			assertThat(supplier).isNotInstanceOf(CachingServiceInstanceListSupplier.class);
			assertThat(supplier).isInstanceOf(HealthCheckServiceInstanceListSupplier.class);
			DelegatingServiceInstanceListSupplier delegating = (DelegatingServiceInstanceListSupplier) supplier;
			assertThat(delegating.getDelegate()).isInstanceOf(DiscoveryClientServiceInstanceListSupplier.class);
		});
	}

	@Import(BaseTestConfig.class)
	private static class CacheTestConfig {

		@Bean
		public LoadBalancerCacheManager cacheManager() {
			return mock(LoadBalancerCacheManager.class);
		}

	}

	private static class BaseTestConfig {

		@Bean
		public ReactiveDiscoveryClient reactiveDiscoveryClient() {
			return mock(ReactiveDiscoveryClient.class);
		}

		@Bean
		public LoadBalancerClientFactory loadBalancerClientFactory() {
			return new LoadBalancerClientFactory(new LoadBalancerClientsProperties());
		}

		@Bean
		public WebClient.Builder webClientBuilder() {
			return WebClient.builder();
		}

	}

}
