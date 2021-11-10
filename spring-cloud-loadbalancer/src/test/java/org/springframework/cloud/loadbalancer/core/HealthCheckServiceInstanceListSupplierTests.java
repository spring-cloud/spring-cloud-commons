/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.loadbalancer.core.ServiceInstanceListSuppliersTestUtils.healthCheckFunction;

/**
 * Tests for {@link HealthCheckServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Roman Matiushchenko
 * @author Roman Chigvintsev
 * @author Sabyasachi Bhattacharya
 */
@SpringBootTest(classes = HealthCheckServiceInstanceListSupplierTests.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthCheckServiceInstanceListSupplierTests {

	private static final String SERVICE_ID = "ignored-service";

	private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(10);

	@LocalServerPort
	private int port;

	private final WebClient webClient = WebClient.create();

	private final RestTemplate restTemplate = new RestTemplate();

	private LoadBalancerProperties.HealthCheck healthCheck;

	private HealthCheckServiceInstanceListSupplier listSupplier;

	@BeforeEach
	void setUp() {
		healthCheck = new LoadBalancerProperties.HealthCheck();
	}

	@AfterEach
	void tearDown() {
		if (listSupplier != null) {
			listSupplier.destroy();
			listSupplier = null;
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPath() {
		String serviceId = "ignored-service";
		healthCheck.getPath().put("ignored-service", "/health");
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1", port,
				false);
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck,
				healthCheckFunction(webClient));

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@Test
	void shouldNotCheckInstanceWithNullHealthCheckPath() {
		BiFunction<ServiceInstance, String, Mono<Boolean>> mockAliveFunction = mock(BiFunction.class);
		String serviceId = "no-health-check-service";
		healthCheck.getPath().put("no-health-check-service", null);
		ServiceInstance serviceInstance = new DefaultServiceInstance("no-health-check-service-1", serviceId,
				"127.0.0.1", port, false);
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck, mockAliveFunction);

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		verify(mockAliveFunction, never()).apply(any(), any());
		assertThat(alive).isTrue();
	}

	@Test
	void shouldNotCheckInstanceWithEmptyHealthCheckPath() {
		BiFunction<ServiceInstance, String, Mono<Boolean>> mockAliveFunction = mock(BiFunction.class);
		String serviceId = "no-health-check-service";
		healthCheck.getPath().put("no-health-check-service", "");
		ServiceInstance serviceInstance = new DefaultServiceInstance("no-health-check-service-1", serviceId,
				"127.0.0.1", port, false);
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck, mockAliveFunction);

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		verify(mockAliveFunction, never()).apply(any(), any());
		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPathWithRestTemplate() {
		String serviceId = "ignored-service";
		healthCheck.getPath().put("ignored-service", "/health");
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1", port,
				false);
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck,
				healthCheckFunction(restTemplate));

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithDefaultHealthCheckPath() {
		String serviceId = "ignored-service";
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1", port,
				false);
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck,
				healthCheckFunction(webClient));

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldReturnFalseIfEndpointNotFound() {
		String serviceId = "ignored-service";
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1", port,
				false);
		healthCheck.getPath().put(serviceId, "/test");
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck,
				healthCheckFunction(webClient));

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isFalse();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldReturnFalseIfEndpointNotFoundWithRestTemplate() {
		String serviceId = "ignored-service";
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1", port,
				false);
		healthCheck.getPath().put(serviceId, "/test");
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck,
				healthCheckFunction(restTemplate));

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isFalse();
	}

	@Test
	void shouldReturnOnlyAliveService() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));

		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1, serviceInstance2)));

			HealthCheckServiceInstanceListSupplier mock = mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(serviceInstance1);
			Mockito.doReturn(Mono.just(false)).when(mock).isAlive(serviceInstance2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.expectNoEvent(healthCheck.getInterval()).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldEmitOnEachAliveServiceInBatch() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1, serviceInstance2)));

			HealthCheckServiceInstanceListSupplier mock = mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(serviceInstance1);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(serviceInstance2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.expectNext(Lists.list(serviceInstance1, serviceInstance2)).expectNoEvent(healthCheck.getInterval())
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldNotFailIfIsAliveReturnsError() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1, serviceInstance2)));

			HealthCheckServiceInstanceListSupplier mock = mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(true)).when(mock).isAlive(serviceInstance1);
			Mockito.doReturn(Mono.error(new RuntimeException("boom"))).when(mock).isAlive(serviceInstance2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.expectNoEvent(healthCheck.getInterval()).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldEmitAllInstancesIfAllIsAliveChecksFailed() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1, serviceInstance2)));
			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					if (serviceInstance == serviceInstance1) {
						return Mono.just(false);
					}
					else {
						return Mono.error(new RuntimeException("boom"));
					}
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list())
				.expectNoEvent(healthCheck.getInterval()).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldMakeInitialDaleyAfterPropertiesSet() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1)));
			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}
			};

			listSupplier.afterPropertiesSet();

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.expectNoEvent(healthCheck.getInterval()).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldRepeatIsAliveChecksIndefinitely() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1, serviceInstance2)));

			HealthCheckServiceInstanceListSupplier mock = mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.doReturn(Mono.just(false), Mono.just(true)).when(mock).isAlive(serviceInstance1);
			Mockito.doReturn(Mono.error(new RuntimeException("boom"))).when(mock).isAlive(serviceInstance2);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list())
				.expectNoEvent(healthCheck.getInterval()).expectNext(Lists.list(serviceInstance1))
				.expectNoEvent(healthCheck.getInterval()).expectNext(Lists.list(serviceInstance1)).thenCancel()
				.verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldTimeoutIsAliveCheck() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1)));

			HealthCheckServiceInstanceListSupplier mock = mock(HealthCheckServiceInstanceListSupplier.class);
			Mockito.when(mock.isAlive(serviceInstance1)).thenReturn(Mono.never(), Mono.just(true));

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return mock.isAlive(serviceInstance);
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNoEvent(healthCheck.getInterval())
				.expectNext(Lists.list()).expectNoEvent(healthCheck.getInterval())
				.expectNext(Lists.list(serviceInstance1)).expectNoEvent(healthCheck.getInterval())
				.expectNext(Lists.list(serviceInstance1)).thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldUpdateInstances() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Flux<List<ServiceInstance>> instances = Flux.just(Lists.list(serviceInstance1))
					.concatWith(Flux.just(Lists.list(serviceInstance1, serviceInstance2))
							.delayElements(healthCheck.getInterval().dividedBy(2)));
			Mockito.when(delegate.get()).thenReturn(instances);

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}
			};

			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.thenAwait(healthCheck.getInterval().dividedBy(2)).expectNext(Lists.list(serviceInstance1))
				.expectNext(Lists.list(serviceInstance1, serviceInstance2)).expectNoEvent(healthCheck.getInterval())
				.expectNext(Lists.list(serviceInstance1)).expectNext(Lists.list(serviceInstance1, serviceInstance2))
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldRefetchInstances() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		healthCheck.setRepeatHealthCheck(false);
		healthCheck.setRefetchInstancesInterval(Duration.ofSeconds(1));
		healthCheck.setRefetchInstances(true);
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			when(delegate.get()).thenReturn(Flux.just(Collections.singletonList(serviceInstance1)))
					.thenReturn(Flux.just(Collections.singletonList(serviceInstance2)));
			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}
			};
			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.thenAwait(healthCheck.getRefetchInstancesInterval()).expectNext(Lists.list(serviceInstance2))
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldRefetchInstancesWithRepeatingHealthCheck() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		healthCheck.setRepeatHealthCheck(true);
		healthCheck.setRefetchInstancesInterval(Duration.ofSeconds(1));
		healthCheck.setRefetchInstances(true);
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);
		ServiceInstance serviceInstance2 = new DefaultServiceInstance("ignored-service-2", SERVICE_ID, "127.0.0.2",
				port, false);

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			when(delegate.get()).thenReturn(Flux.just(Collections.singletonList(serviceInstance1)))
					.thenReturn(Flux.just(Collections.singletonList(serviceInstance2)));
			BiFunction<ServiceInstance, String, Mono<Boolean>> healthCheckFunc = healthCheckFunction(webClient);
			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck, healthCheckFunc) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}
			};
			return listSupplier.get();
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.thenAwait(healthCheck.getRefetchInstancesInterval()).expectNext(Lists.list(serviceInstance2))
				.thenCancel().verify(VERIFY_TIMEOUT);
	}

	@Test
	void shouldCacheResultIfAfterPropertiesSetInvoked() {
		healthCheck.setInitialDelay(Duration.ofSeconds(1));
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("ignored-service-1", SERVICE_ID, "127.0.0.1",
				port, false);

		AtomicInteger emitCounter = new AtomicInteger();

		StepVerifier.withVirtualTime(() -> {
			ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
			Mockito.when(delegate.getServiceId()).thenReturn(SERVICE_ID);
			Mockito.when(delegate.get()).thenReturn(Flux.just(Lists.list(serviceInstance1)));

			listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
					healthCheckFunction(webClient)) {
				@Override
				protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
					return Mono.just(true);
				}

				@Override
				protected Flux<List<ServiceInstance>> healthCheckFlux(List<ServiceInstance> instances) {
					return super.healthCheckFlux(instances).doOnNext(it -> emitCounter.incrementAndGet());
				}
			};

			listSupplier.afterPropertiesSet();

			return listSupplier.get().take(1).concatWith(listSupplier.get().take(1));
		}).expectSubscription().expectNoEvent(healthCheck.getInitialDelay()).expectNext(Lists.list(serviceInstance1))
				.expectNext(Lists.list(serviceInstance1)).thenCancel().verify(VERIFY_TIMEOUT);

		Assertions.assertThat(emitCounter).hasValue(1);
	}

	@Test
	void shouldCancelSubscription() {

		final AtomicInteger instancesCanceled = new AtomicInteger();
		final AtomicBoolean subscribed = new AtomicBoolean();
		ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
		Mockito.when(delegate.get()).thenReturn(Flux.<List<ServiceInstance>>never()
				.doOnSubscribe(subscription -> subscribed.set(true)).doOnCancel(instancesCanceled::incrementAndGet));

		listSupplier = new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
				healthCheckFunction(webClient));

		listSupplier.afterPropertiesSet();

		Awaitility.await("delegate subscription").pollDelay(Duration.ofMillis(50)).atMost(VERIFY_TIMEOUT)
				.untilTrue(subscribed);

		Assertions.assertThat(instancesCanceled).hasValue(0);

		listSupplier.destroy();
		Awaitility.await("delegate cancellation").pollDelay(Duration.ofMillis(100)).atMost(VERIFY_TIMEOUT)
				.untilAsserted(() -> Assertions.assertThat(instancesCanceled).hasValue(1));
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCheckInstanceWithProvidedHealthCheckPathWithQueryParams() {
		String serviceId = "ignored-service";
		healthCheck.getPath().put("ignored-service", "/health?someparam=somevalue");
		ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1", port,
				false);
		listSupplier = new HealthCheckServiceInstanceListSupplier(
				ServiceInstanceListSuppliers.from(serviceId, serviceInstance), healthCheck,
				healthCheckFunction(webClient));

		boolean alive = listSupplier.isAlive(serviceInstance).block();

		assertThat(alive).isTrue();
	}

	@Test
	void shouldCheckUseProvidedPortForHealthCheckRequest() {
		Throwable exception = catchThrowable(() -> {
			String serviceId = "ignored-service";
			healthCheck.setPort(8888);
			LoadBalancerProperties properties = new LoadBalancerProperties();
			properties.setHealthCheck(healthCheck);
			LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);
			when(loadBalancerClientFactory.getProperties(serviceId)).thenReturn(properties);
			ServiceInstance serviceInstance = new DefaultServiceInstance("ignored-service-1", serviceId, "127.0.0.1",
					port, false);
			listSupplier = new HealthCheckServiceInstanceListSupplier(
					ServiceInstanceListSuppliers.from(serviceId, serviceInstance), loadBalancerClientFactory,
					healthCheckFunction(webClient));

			listSupplier.isAlive(serviceInstance).block();
		});

		assertThat(exception).hasMessageContaining("Connection refused: /127.0.0.1:888");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(HealthCheckServiceInstanceListSupplierTests.TestApplication.class, args);
		}

		@GetMapping("/health")
		void healthCheck(@RequestParam(value = "someparam", required = false) String param) {

		}

		@GetMapping("/actuator/health")
		void defaultHealthCheck() {

		}

	}

}
