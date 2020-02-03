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

package org.springframework.cloud.loadbalancer.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthCheckServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = HealthCheckServiceInstanceListSupplierTests.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthCheckServiceInstanceListSupplierTests {

	@LocalServerPort
	private int port;

	private final WebClient webClient = WebClient.create();

	private LoadBalancerProperties.HealthCheck healthCheck = new LoadBalancerProperties.HealthCheck();

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPath() {
		healthCheck.getPath().put("ignored-service", "/health");
		HealthCheckServiceInstanceListSupplier listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSupplier.FixedServiceInstanceListSupplier
						.with(new MockEnvironment()).build(),
				healthCheck, webClient);
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithDefaultHealthCheckPath() {
		HealthCheckServiceInstanceListSupplier listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSupplier.FixedServiceInstanceListSupplier
						.with(new MockEnvironment()).build(),
				healthCheck, webClient);
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldReturnFalseIfEndpointNotFound() {
		healthCheck.getPath().put("ignored-service", "/test");
		HealthCheckServiceInstanceListSupplier listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSupplier.FixedServiceInstanceListSupplier
						.with(new MockEnvironment()).build(),
				healthCheck, webClient);
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(
					HealthCheckServiceInstanceListSupplierTests.TestApplication.class,
					args);
		}

		@GetMapping("/health")
		void healthCheck() {

		}

		@GetMapping("/actuator/health")
		void defaultHealthCheck() {

		}

	}

}
