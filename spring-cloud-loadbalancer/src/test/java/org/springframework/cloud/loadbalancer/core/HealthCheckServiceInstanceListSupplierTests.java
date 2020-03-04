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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
 * @author Roman Matiushchenko
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = HealthCheckServiceInstanceListSupplierTests.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthCheckServiceInstanceListSupplierTests {

	private static final String SERVICE_ID = "ignored-service";

	private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(10);

	@LocalServerPort
	private int port;

	private final WebClient webClient = WebClient.create();

	private LoadBalancerProperties.HealthCheck healthCheck;

	private HealthCheckServiceInstanceListSupplier listSupplier;

	@BeforeEach
	void setUp() {
		healthCheck = new LoadBalancerProperties.HealthCheck();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (listSupplier != null) {
			listSupplier.destroy();
			listSupplier = null;
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPath() {
		healthCheck.getPath().put("ignored-service", "/health");
		listSupplier = new HealthCheckServiceInstanceListSupplier(
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
		listSupplier = new HealthCheckServiceInstanceListSupplier(
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
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSupplier.FixedServiceInstanceListSupplier
						.with(new MockEnvironment()).build(),
				healthCheck, webClient);
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1",
				"ignored-service", "127.0.0.1", port, false);

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isFalse();
	}

	@Test
	void shouldReturnOnlyAliveService() {
		healthCheck.setInitialDelay(1000);

		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);
		ServiceInstance si2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID,
				"127.0.0.2", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1, si2)));

			HealthCheckServiceInstanceListSupplier mock = Mockito
					.mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(si1);
			Mockito.doReturn(Mono.just(false)).when(mock).isAlive(si2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list(si1)).expectNoEvent(healthCheck.getInterval())
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldEmitOnEachAliveServiceInBatch() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);
		ServiceInstance si2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID,
				"127.0.0.2", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1, si2)));

			HealthCheckServiceInstanceListSupplier mock = Mockito
					.mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(si1);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(si2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list(si1)).expectNext(Lists.list(si1, si2))
				.expectNoEvent(healthCheck.getInterval()).thenCancel()
				.verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldNotFailIfIsAliveReturnsError() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);
		ServiceInstance si2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID,
				"127.0.0.2", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1, si2)));

			HealthCheckServiceInstanceListSupplier mock = Mockito
					.mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(si1);
			Mockito.doReturn(Mono.error(new RuntimeException("boom"))).when(mock)
					.isAlive(si2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list(si1)).expectNoEvent(healthCheck.getInterval())
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldEmitAllInstancesIfAllIsAliveChecksFailed() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);
		ServiceInstance si2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID,
				"127.0.0.2", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1, si2)));
			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					if (serviceInstance == si1) {
						return Mono.just(false);
					}
					else {
						return Mono.error(new RuntimeException("boom"));
					}
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list()).expectNoEvent(healthCheck.getInterval())
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldMakeInitialDaleyAfterPropertiesSet() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1)));
			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}
			};

			listSupplier.afterPropertiesSet();

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list(si1)).expectNoEvent(healthCheck.getInterval())
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldRepeatIsAliveChecksIndefinitely() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);
		ServiceInstance si2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID,
				"127.0.0.2", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1, si2)));

			HealthCheckServiceInstanceListSupplier mock = Mockito
					.mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(false), Mono.just(true)).when(mock).isAlive(si1);
			Mockito.doReturn(Mono.error(new RuntimeException("boom"))).when(mock)
					.isAlive(si2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list()).expectNoEvent(healthCheck.getInterval())
				.expectNext(Lists.list(si1)).expectNoEvent(healthCheck.getInterval())
				.expectNext(Lists.list(si1)).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldTimeoutIsAliveCheck() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1)));

			HealthCheckServiceInstanceListSupplier mock = Mockito
					.mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.when(mock.isAlive(si1)).thenReturn(Mono.never(), Mono.just(true));

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNoEvent(healthCheck.getInterval()).expectNext(Lists.list())
				.expectNoEvent(healthCheck.getInterval()).expectNext(Lists.list(si1))
				.expectNoEvent(healthCheck.getInterval()).expectNext(Lists.list(si1))
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldUpdateInstances() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);
		ServiceInstance si2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID,
				"127.0.0.2", port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Flux<List<ServiceInstance>> instances = Flux.just(Lists.list(si1))
					.concatWith(Flux.just(Lists.list(si1, si2))
							.delayElements(healthCheck.getInterval().dividedBy(2)));
			Mockito.when(delegate.get()).thenReturn(instances);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}
			};

			return listSupplier.get();
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list(si1))
				.thenAwait(healthCheck.getInterval().dividedBy(2))
				.expectNext(Lists.list(si1)).expectNext(Lists.list(si1, si2))
				.expectNoEvent(healthCheck.getInterval()).expectNext(Lists.list(si1))
				.expectNext(Lists.list(si1, si2)).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldCacheResultIfAfterPropertiesSetInvoked() {
		healthCheck.setInitialDelay(1000);
		ServiceInstance si1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID,
				"127.0.0.1", port, false);

		AtomicInteger emitCounter = new AtomicInteger();

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = Mockito
					.mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(si1)));

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate,
					healthCheck, webClient) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}

				@Override
				protected Flux<List<ServiceInstance>> healthCheckFlux(
						List<ServiceInstance> instances) {
					return super.healthCheckFlux(instances)
							.doOnNext(it -> emitCounter.incrementAndGet());
				}
			};

			listSupplier.afterPropertiesSet();

			return listSupplier.get().take(1).concatWith(listSupplier.get().take(1));
		}).expectSubscription()
				.expectNoEvent(Duration.ofMillis(healthCheck.getInitialDelay()))
				.expectNext(Lists.list(si1)).expectNext(Lists.list(si1)).thenCancel()
				.verify(VERIFY_TIMEOUT);

		Assertions.assertThat(emitCounter).hasValue(1);
	}

	@Test
	void shouldCancelSubscription() {

		final AtomicInteger instancesCanceled = new AtomicInteger();

		ServiceInstanceListSupplier delegate = Mockito
				.mock(ServiceInstanceListSupplier.class);
		Mockito.when(delegate.get()).thenReturn(Flux.<List<ServiceInstance>>never()
				.log("test").doOnCancel(instancesCanceled::incrementAndGet));

		listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
				webClient);

		listSupplier.afterPropertiesSet();

		Assertions.assertThat(instancesCanceled).hasValue(0);

		listSupplier.destroy();
		Awaitility.await().pollDelay(Duration.ofMillis(100)).atMost(VERIFY_TIMEOUT)
				.untilAsserted(
						() -> Assertions.assertThat(instancesCanceled).hasValue(1));
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
