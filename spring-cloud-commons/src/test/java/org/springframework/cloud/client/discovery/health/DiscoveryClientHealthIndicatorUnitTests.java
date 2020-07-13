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

package org.springframework.cloud.client.discovery.health;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DiscoveryClientHealthIndicator}.
 *
 * @author Chris Bono
 */
@ExtendWith(MockitoExtension.class)
class DiscoveryClientHealthIndicatorUnitTests {

	@Mock
	private ObjectProvider<DiscoveryClient> discoveryClientProvider;

	@Mock
	private DiscoveryClient discoveryClient;

	@Mock
	private DiscoveryClientHealthIndicatorProperties properties;

	@InjectMocks
	private DiscoveryClientHealthIndicator indicator;

	@BeforeEach
	public void prepareMocks() {
		lenient().when(discoveryClientProvider.getIfAvailable())
				.thenReturn(discoveryClient);
	}

	@Test
	public void shouldReturnUnknownStatusWhenNotInitialized() {
		Health expectedHealth = Health.status(
				new Status(Status.UNKNOWN.getCode(), "Discovery Client not initialized"))
				.build();
		Health health = indicator.health();
		assertThat(health).isEqualTo(expectedHealth);
	}

	@Test
	public void shouldReturnUpStatusWhenNotUsingServicesQueryAndProbeSucceeds() {
		when(properties.isUseServicesQuery()).thenReturn(false);
		Health expectedHealth = Health.status(new Status(Status.UP.getCode(), ""))
				.build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Health health = indicator.health();

		assertThat(health).isEqualTo(expectedHealth);
	}

	@Test
	public void shouldReturnDownStatusWhenNotUsingServicesQueryAndProbeFails() {
		when(properties.isUseServicesQuery()).thenReturn(false);
		RuntimeException ex = new RuntimeException("something went wrong");
		doThrow(ex).when(discoveryClient).probe();
		Health expectedHealth = Health.down(ex).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Health health = indicator.health();

		assertThat(health).isEqualTo(expectedHealth);
	}

	@Test
	public void shouldReturnUpStatusWhenUsingServicesQueryAndNoServicesReturned() {
		when(properties.isUseServicesQuery()).thenReturn(true);
		when(discoveryClient.getServices()).thenReturn(Collections.emptyList());
		Health expectedHealth = Health.status(new Status(Status.UP.getCode(), ""))
				.withDetail("services", emptyList()).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Health health = indicator.health();

		assertThat(health).isEqualTo(expectedHealth);
	}

	@Test
	public void shouldReturnUpStatusWhenUsingServicesQueryAndServicesReturned() {
		when(properties.isUseServicesQuery()).thenReturn(true);
		when(properties.isIncludeDescription()).thenReturn(true);
		when(discoveryClient.description()).thenReturn("Mocked Service Discovery Client");
		when(discoveryClient.getServices()).thenReturn(singletonList("service"));
		Health expectedHealth = Health
				.status(new Status(Status.UP.getCode(),
						"Mocked Service Discovery Client"))
				.withDetail("services", singletonList("service")).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Health health = indicator.health();

		assertThat(health).isEqualTo(expectedHealth);
	}

	@Test
	public void shouldReturnDownStatusWhenUsingServicesQueryAndCallFails() {
		when(properties.isUseServicesQuery()).thenReturn(true);
		RuntimeException ex = new RuntimeException("something went wrong");
		when(discoveryClient.getServices()).thenThrow(ex);
		Health expectedHealth = Health.down(ex).build();

		indicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));
		Health health = indicator.health();

		assertThat(health).isEqualTo(expectedHealth);
	}

}
