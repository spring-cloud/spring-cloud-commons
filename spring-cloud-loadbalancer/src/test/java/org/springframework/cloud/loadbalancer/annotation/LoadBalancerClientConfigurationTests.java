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

package org.springframework.cloud.loadbalancer.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerCacheAutoConfiguration;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.HealthCheckServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for {@link LoadBalancerClientConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 */
class LoadBalancerClientConfigurationTests {

	ApplicationContextRunner reactiveDiscoveryClientRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ReactiveCompositeDiscoveryClientAutoConfiguration.class,
					LoadBalancerCacheAutoConfiguration.class,
					LoadBalancerAutoConfiguration.class,
					LoadBalancerClientConfiguration.class));

	ApplicationContextRunner blockingDiscoveryClientRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(CompositeDiscoveryClientAutoConfiguration.class,
							LoadBalancerCacheAutoConfiguration.class,
							LoadBalancerAutoConfiguration.class,
							LoadBalancerClientConfiguration.class));

	@Test
	void shouldInstantiateDefaultServiceInstanceListSupplierWhenConfigurationsPropertyNotSet() {
		reactiveDiscoveryClientRunner.run(context -> {
			ServiceInstanceListSupplier supplier = context
					.getBean(ServiceInstanceListSupplier.class);
			then(supplier).isInstanceOf(CachingServiceInstanceListSupplier.class);
			then(((DelegatingServiceInstanceListSupplier) supplier).getDelegate())
					.isInstanceOf(DiscoveryClientServiceInstanceListSupplier.class);
		});
	}

	@Test
	void shouldInstantiateDefaultServiceInstanceListSupplier() {
		reactiveDiscoveryClientRunner
				.withPropertyValues("spring.cloud.loadbalancer.configurations=default")
				.run(context -> {
					ServiceInstanceListSupplier supplier = context
							.getBean(ServiceInstanceListSupplier.class);
					then(supplier).isInstanceOf(CachingServiceInstanceListSupplier.class);
					then(((DelegatingServiceInstanceListSupplier) supplier).getDelegate())
							.isInstanceOf(
									DiscoveryClientServiceInstanceListSupplier.class);
				});
	}

	@Test
	void shouldInstantiateZonePreferenceServiceInstanceListSupplier() {
		reactiveDiscoveryClientRunner
				.withPropertyValues(
						"spring.cloud.loadbalancer.configurations=zone-preference")
				.run(context -> {
					ServiceInstanceListSupplier supplier = context
							.getBean(ServiceInstanceListSupplier.class);
					then(supplier).isInstanceOf(CachingServiceInstanceListSupplier.class);
					ServiceInstanceListSupplier delegate = ((DelegatingServiceInstanceListSupplier) supplier)
							.getDelegate();
					then(delegate).isInstanceOf(
							ZonePreferenceServiceInstanceListSupplier.class);
					ServiceInstanceListSupplier secondDelegate = ((DelegatingServiceInstanceListSupplier) delegate)
							.getDelegate();
					then(secondDelegate).isInstanceOf(
							DiscoveryClientServiceInstanceListSupplier.class);
				});
	}

	@Test
	void shouldInstantiateHealthCheckServiceInstanceListSupplier() {
		reactiveDiscoveryClientRunner.withUserConfiguration(TestConfig.class)
				.withPropertyValues(
						"spring.cloud.loadbalancer.configurations=health-check")
				.run(context -> {
					ServiceInstanceListSupplier supplier = context
							.getBean(ServiceInstanceListSupplier.class);
					then(supplier)
							.isInstanceOf(HealthCheckServiceInstanceListSupplier.class);
					ServiceInstanceListSupplier delegate = ((DelegatingServiceInstanceListSupplier) supplier)
							.getDelegate();
					then(delegate).isInstanceOf(
							DiscoveryClientServiceInstanceListSupplier.class);
				});
	}

	@Test
	void shouldInstantiateDefaultBlockingServiceInstanceListSupplierWhenConfigurationsPropertyNotSet() {
		blockingDiscoveryClientRunner.run(context -> {
			ServiceInstanceListSupplier supplier = context
					.getBean(ServiceInstanceListSupplier.class);
			then(supplier).isInstanceOf(CachingServiceInstanceListSupplier.class);
			then(((DelegatingServiceInstanceListSupplier) supplier).getDelegate())
					.isInstanceOf(DiscoveryClientServiceInstanceListSupplier.class);
		});
	}

	@Test
	void shouldInstantiateDefaultBlockingServiceInstanceListSupplier() {
		blockingDiscoveryClientRunner
				.withPropertyValues("spring.cloud.loadbalancer.configurations=default")
				.run(context -> {
					ServiceInstanceListSupplier supplier = context
							.getBean(ServiceInstanceListSupplier.class);
					then(supplier).isInstanceOf(CachingServiceInstanceListSupplier.class);
					then(((DelegatingServiceInstanceListSupplier) supplier).getDelegate())
							.isInstanceOf(
									DiscoveryClientServiceInstanceListSupplier.class);
				});
	}

	@Test
	void shouldInstantiateServiceInstanceSupplierRegardlessOfConfigurationProperty() {
		reactiveDiscoveryClientRunner
				.withPropertyValues(
						"spring.cloud.loadbalancer.configurations=zone-preference")
				.run(context -> {
					ServiceInstanceSupplier supplier = context
							.getBean(ServiceInstanceSupplier.class);
					then(supplier).isInstanceOf(CachingServiceInstanceSupplier.class);
				});
	}

	@Configuration
	protected static class TestConfig {

		@Bean
		@LoadBalanced
		WebClient.Builder webClientBuilder() {
			return WebClient.builder();
		}

	}

}
