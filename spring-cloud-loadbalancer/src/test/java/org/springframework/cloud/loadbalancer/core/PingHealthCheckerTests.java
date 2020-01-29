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
import org.springframework.web.reactive.function.client.WebClient;

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

	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPath() {
		PingHealthChecker healthChecker = new PingHealthChecker(WebClient.builder()
				.build(), new MockEnvironment()
				.withProperty("spring.cloud.loadbalancer.ignored-service.healthcheck.path", "/health"));
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = healthChecker.isAlive(serviceInstance);

		assertThat(alive).isTrue();
	}

	@Test
	void shouldCheckInstanceWithDefaultHealthCheckPath() {
		PingHealthChecker healthChecker = new PingHealthChecker(WebClient.builder()
				.build(), new MockEnvironment());
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = healthChecker.isAlive(serviceInstance);

		assertThat(alive).isTrue();
	}

	@Test
	void shouldReturnFalseIfEndpointNotFound() {
		PingHealthChecker healthChecker = new PingHealthChecker(WebClient.builder()
				.build(), new MockEnvironment()
				.withProperty("spring.cloud.loadbalancer.ignored-service.healthcheck.path", "/test"));
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = healthChecker.isAlive(serviceInstance);

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

