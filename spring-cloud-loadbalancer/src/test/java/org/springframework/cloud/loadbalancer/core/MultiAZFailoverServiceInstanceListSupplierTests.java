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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MultiAZFailoverServiceInstanceListSupplier}.
 *
 * @author Jiwon Jeon
 */
class MultiAZFailoverServiceInstanceListSupplierTests {

	private static final String METADATA_KEY_ZONE = "zone";

	private static final String CACHE_KEY_FAILED = "FAILED";

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private final LoadBalancerZoneConfig zoneConfig = new LoadBalancerZoneConfig("zone1", List.of("zone2", "zone3"));

	private final ServiceInstance first = serviceInstance("test-1", buildZoneMetadata("zone1"));

	private final ServiceInstance second = serviceInstance("test-2", buildZoneMetadata("zone3"));

	private final ServiceInstance third = serviceInstance("test-3", buildZoneMetadata("zone2"));

	private final ServiceInstance fourth = serviceInstance("test-4", buildZoneMetadata("zone3"));

	private final ServiceInstance fifth = serviceInstance("test-5", buildZoneMetadata("zone1"));

	private final MultiAZFailoverLoadBalancerCacheDataManager cacheDataManager = mock(
			MultiAZFailoverLoadBalancerCacheDataManager.class);

	private final MultiAZFailoverServiceInstanceListSupplier supplier = new MultiAZFailoverServiceInstanceListSupplier(
			delegate, cacheDataManager, zoneConfig);

	@Test
	void shouldFilterInstancesByZoneWhenNoAZFailed() {
		when(cacheDataManager.getInstance(CACHE_KEY_FAILED, Set.class)).thenReturn(Collections.EMPTY_SET);
		when(delegate.get()).thenReturn(Flux.just(List.of(first, second, third, fourth, fifth)));

		supplier.get().as(StepVerifier::create).assertNext(filtered -> {
			assertThat(filtered).hasSize(5);
			assertThat(filtered.get(0).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone1");
			assertThat(filtered.get(1).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone1");
			assertThat(filtered.get(2).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone2");
			assertThat(filtered.get(3).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone3");
			assertThat(filtered.get(4).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone3");
		}).verifyComplete();
	}

	@Test
	void shouldFilterInstancesByZoneWhenAZFailed() {
		when(cacheDataManager.getInstance(CACHE_KEY_FAILED, Set.class))
				.thenReturn(new HashSet<>(List.of(first, fifth)));
		when(delegate.get()).thenReturn(Flux.just(List.of(first, second, third, fourth, fifth)));

		supplier.get().as(StepVerifier::create).assertNext(filtered -> {
			assertThat(filtered).hasSize(3);
			assertThat(filtered.get(0).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone2");
			assertThat(filtered.get(1).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone3");
			assertThat(filtered.get(2).getMetadata().get(METADATA_KEY_ZONE)).isEqualTo("zone3");
		}).verifyComplete();
	}

	private DefaultServiceInstance serviceInstance(String instanceId, Map<String, String> metadata) {
		return new DefaultServiceInstance(instanceId, "test", "http://test.test", 9080, false, metadata);
	}

	private Map<String, String> buildZoneMetadata(String zone) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put(METADATA_KEY_ZONE, zone);
		return metadata;
	}

}
