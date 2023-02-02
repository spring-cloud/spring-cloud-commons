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

package org.springframework.cloud.client.discovery.health.reactive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.core.Ordered;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 * @author Chris Bono
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(MockitoExtension.class)
class ReactiveDiscoveryClientHealthIndicatorTests {

	@Mock
	private ReactiveDiscoveryClient discoveryClient;

	@Mock
	private DiscoveryClientHealthIndicatorProperties properties;

	@InjectMocks
	private ReactiveDiscoveryClientHealthIndicator indicator;

	@Test
	public void shouldReturnCorrectOrder() {
		assertThat(indicator.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
		indicator.setOrder(0);
		assertThat(indicator.getOrder()).isEqualTo(0);
	}

	@Test
	public void shouldUseClientDescriptionForIndicatorName() {
		when(discoveryClient.description()).thenReturn("Mocked Service Discovery Client");
		assertThat(indicator.getName()).isEqualTo("Mocked Service Discovery Client");
	}

	@Test
	public void shouldReturnUnknownStatusWhenNotInitialized() {
		Health expectedHealth = Health.status(new Status(Status.UNKNOWN.getCode(), "Discovery Client not initialized"))
				.build();
		Mono<Health> health = indicator.health();
		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnUpStatusWhenNotUsingServicesQueryAndProbeSucceeds() {
		when(properties.isUseServicesQuery()).thenReturn(false);
		ReactiveDiscoveryClient discoveryClient = new TestDiscoveryClient();
		ReactiveDiscoveryClientHealthIndicator indicator = new ReactiveDiscoveryClientHealthIndicator(discoveryClient,
				properties);
		Health expectedHealth = Health.status(new Status(Status.UP.getCode(), "")).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnDownStatusWhenNotUsingServicesQueryAndProbeFails() {
		ExceptionThrowingDiscoveryClient discoveryClient = new ExceptionThrowingDiscoveryClient();
		ReactiveDiscoveryClientHealthIndicator indicator = new ReactiveDiscoveryClientHealthIndicator(discoveryClient,
				properties);
		Health expectedHealth = Health.down(discoveryClient.exception).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnUpStatusWhenUsingServicesQueryAndNoServicesReturned() {
		when(properties.isUseServicesQuery()).thenReturn(true);
		when(discoveryClient.getServices()).thenReturn(Flux.empty());
		Health expectedHealth = Health.status(new Status(Status.UP.getCode(), "")).withDetail("services", emptyList())
				.build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnUpStatusWhenUsingServicesQueryAndServicesReturned() {
		when(properties.isUseServicesQuery()).thenReturn(true);
		when(properties.isIncludeDescription()).thenReturn(true);
		when(discoveryClient.getServices()).thenReturn(Flux.just("service"));
		when(discoveryClient.description()).thenReturn("Mocked Service Discovery Client");
		Health expectedHealth = Health.status(new Status(Status.UP.getCode(), "Mocked Service Discovery Client"))
				.withDetail("services", singletonList("service")).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnDownStatusWhenUsingServicesQueryAndCallFails() {
		when(properties.isUseServicesQuery()).thenReturn(true);
		RuntimeException ex = new RuntimeException("something went wrong");
		when(discoveryClient.getServices()).thenReturn(Flux.error(ex));
		Health expectedHealth = Health.down(ex).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	static class TestDiscoveryClient implements ReactiveDiscoveryClient {

		@Override
		public String description() {
			return "Test";
		}

		@Override
		public Flux<ServiceInstance> getInstances(String serviceId) {
			return Flux.just(new DefaultServiceInstance());
		}

		@Override
		public Flux<String> getServices() {
			return Flux.just("Test");
		}

	}

	static class ExceptionThrowingDiscoveryClient implements ReactiveDiscoveryClient {

		RuntimeException exception = new RuntimeException("something went wrong");

		@Override
		public String description() {
			return "Exception";
		}

		@Override
		public Flux<ServiceInstance> getInstances(String serviceId) {
			throw new RuntimeException("Test!");
		}

		@Override
		public Flux<String> getServices() {
			throw new RuntimeException("something went wrong");
		}

	}

}
