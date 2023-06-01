/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.client.discovery.composite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito tests for Composite Discovery Client
 *
 * @author Sean Ruffatti
 */
@ExtendWith(MockitoExtension.class)
public class CompositeDiscoveryClientUnitTests {

	private CompositeDiscoveryClient underTest;

	@Mock
	private DiscoveryClient client1;

	@Mock
	private DiscoveryClient client2;

	@BeforeEach
	void setUp() {
		underTest = new CompositeDiscoveryClient(Arrays.asList(client1, client2));
	}

	@Test
	void shouldRetrieveInstancesByServiceId() {
		ServiceInstance serviceInstance1 = new DefaultServiceInstance("instance1", "serviceId", "https://s1", 8443,
				true);
		when(client1.getInstances("serviceId")).thenReturn(Collections.singletonList(serviceInstance1));

		List<ServiceInstance> serviceInstances = underTest.getInstances("serviceId");

		then(serviceInstances.get(0).getInstanceId()).isEqualTo("instance1");
		then(serviceInstances.get(0).getServiceId()).isEqualTo("serviceId");
		then(serviceInstances.get(0).getHost()).isEqualTo("https://s1");
		then(serviceInstances.get(0).getPort()).isEqualTo(8443);
	}

	@Test
	void shouldReturnServiceIds() {
		when(client1.getServices()).thenReturn(Collections.singletonList("serviceId1"));
		when(client2.getServices()).thenReturn(Collections.singletonList("serviceId2"));

		List<String> services = underTest.getServices();

		then(services.size()).isEqualTo(2);
		then(services).containsOnlyOnce("serviceId1", "serviceId2");
	}

	@Test
	void shouldReturnAllDiscoveryClients() {
		then(underTest.getDiscoveryClients()).containsOnlyOnce(client1, client2);
	}

	@Test
	void shouldCallProbeOnAllDiscoveryClients() {
		underTest.probe();

		// Every DiscoveryClient bean should invoke DiscoveryClient.probe() when
		// CompositeDiscoveryClient.probe() is invoked.
		verify(client1, times(1)).probe();
		verify(client1, times(0)).getServices();
		verify(client2, times(1)).probe();
		verify(client2, times(0)).getServices();
	}

}
