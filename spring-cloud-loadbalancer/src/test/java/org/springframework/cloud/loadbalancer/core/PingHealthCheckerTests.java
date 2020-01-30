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
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PingHealthCheckerTests.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingHealthCheckerTests {

	@LocalServerPort
	private int port;

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPath() {
		PingHealthChecker healthChecker = new PingHealthChecker(
				new MockEnvironment().withProperty(
						"spring.cloud.loadbalancer.ignored-service.healthcheck.path",
						"/health"));
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = healthChecker.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithDefaultHealthCheckPath() {
		PingHealthChecker healthChecker = new PingHealthChecker(new MockEnvironment());
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = healthChecker.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldReturnFalseIfEndpointNotFound() {
		PingHealthChecker healthChecker = new PingHealthChecker(
				new MockEnvironment().withProperty(
						"spring.cloud.loadbalancer.ignored-service.healthcheck.path",
						"/test"));
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = healthChecker.isAlive(serviceInstance).block();

		assertThat(alive).isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

		@GetMapping("/health")
		void healthCheck() {

		}

		@GetMapping("/actuator/health")
		void defaultHealthCheck() {

		}

	}

}
