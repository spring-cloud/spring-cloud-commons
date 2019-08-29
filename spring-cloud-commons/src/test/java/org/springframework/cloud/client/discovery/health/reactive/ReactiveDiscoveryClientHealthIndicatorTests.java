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
	public void shouldReturnUnknownStatusWhenNotInitialized() {
		Health expectedHealth = Health.status(
				new Status(Status.UNKNOWN.getCode(), "Discovery Client not initialized"))
				.build();
		Mono<Health> health = indicator.health();
		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnUpStatusWithoutServices() {
		when(discoveryClient.description()).thenReturn("Mocked Service Discovery Client");
		when(discoveryClient.getServices()).thenReturn(Flux.empty());
		Health expectedHealth = Health.status(new Status(Status.UP.getCode(), ""))
				.withDetail("services", emptyList()).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		assertThat(indicator.getName()).isEqualTo("Mocked Service Discovery Client");
		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnUpStatusWithServices() {
		when(discoveryClient.getServices()).thenReturn(Flux.just("service"));
		when(properties.isIncludeDescription()).thenReturn(true);
		when(discoveryClient.description()).thenReturn("Mocked Service Discovery Client");
		Health expectedHealth = Health
				.status(new Status(Status.UP.getCode(),
						"Mocked Service Discovery Client"))
				.withDetail("services", singletonList("service")).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		assertThat(indicator.getName()).isEqualTo("Mocked Service Discovery Client");
		StepVerifier.create(health).expectNext(expectedHealth).expectComplete().verify();
	}

	@Test
	public void shouldReturnDownStatusWhenServicesCouldNotBeRetrieved() {
		RuntimeException ex = new RuntimeException("something went wrong");
		Health expectedHealth = Health.down(ex).build();
		when(discoveryClient.getServices()).thenReturn(Flux.error(ex));

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Mono<Health> health = indicator.health();

		StepVerifier.create(health).expectNext(expectedHealth).expectComplete()
				.verifyThenAssertThat();
	}

}
