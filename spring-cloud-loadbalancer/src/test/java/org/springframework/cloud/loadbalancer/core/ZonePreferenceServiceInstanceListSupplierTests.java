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

package org.springframework.cloud.loadbalancer.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ZonePreferenceServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
class ZonePreferenceServiceInstanceListSupplierTests {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private final LoadBalancerZoneConfig zoneConfig = new LoadBalancerZoneConfig(null);

	private ZonePreferenceServiceInstanceListSupplier supplier;

	private final LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);

	private final ServiceInstance first = serviceInstance("test-1", buildZoneMetadata("zone1"));

	private final ServiceInstance second = serviceInstance("test-2", buildZoneMetadata("zone1"));

	private final ServiceInstance third = serviceInstance("test-3", buildZoneMetadata("zone2"));

	private final ServiceInstance fourth = serviceInstance("test-4", buildZoneMetadata("zone3"));

	private final ServiceInstance fifth = serviceInstance("test-5", buildZoneMetadata(null));

	@BeforeEach
	void setUp() {
		LoadBalancerProperties properties = new LoadBalancerProperties();
		properties.setCallGetWithRequestOnDelegates(true);
		when(loadBalancerClientFactory.getProperties(any())).thenReturn(properties);
		supplier = new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig, loadBalancerClientFactory);
	}

	@Test
	void shouldFilterInstancesByZone() {
		zoneConfig.setZone("zone1");
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth, fifth)));

		List<ServiceInstance> filtered = supplier.get().blockFirst();

		assertThat(filtered).hasSize(2);
		assertThat(filtered).contains(first, second);
		assertThat(filtered).doesNotContain(third);
		assertThat(filtered).doesNotContain(fourth);
		assertThat(filtered).doesNotContain(fifth);
	}

	@Test
	void shouldCallGetRequestOnDelegate() {
		zoneConfig.setZone("zone1");
		Request<DefaultRequestContext> request = new DefaultRequest<>(new DefaultRequestContext());
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth, fifth)));
		when(delegate.get(request)).thenReturn(Flux.just(Arrays.asList(first, third, fourth, fifth)));

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(1);
		assertThat(filtered).containsOnly(first);
	}

	@Test
	void shouldReturnAllInstancesIfNoZoneInstances() {
		zoneConfig.setZone("zone1");
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(third, fourth)));

		List<ServiceInstance> filtered = supplier.get().blockFirst();

		assertThat(filtered).hasSize(2);
		assertThat(filtered).contains(third, fourth);
	}

	@Test
	void shouldNotThrowNPEIfNullInstanceMetadata() {
		zoneConfig.setZone("zone1");
		when(delegate.get()).thenReturn(Flux.just(Collections.singletonList(serviceInstance("test-6", null))));
		assertThatCode(() -> supplier.get().blockFirst()).doesNotThrowAnyException();
	}

	@Test
	void shouldReturnAllInstancesIfNoZone() {
		zoneConfig.setZone(null);
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth)));

		List<ServiceInstance> filtered = supplier.get().blockFirst();

		assertThat(filtered).hasSize(4);
		assertThat(filtered).contains(first, second, third, fourth);
	}

	private DefaultServiceInstance serviceInstance(String instanceId, Map<String, String> metadata) {
		return new DefaultServiceInstance(instanceId, "test", "http://test.test", 9080, false, metadata);
	}

	private Map<String, String> buildZoneMetadata(String zone) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("zone", zone);
		return metadata;
	}

}
