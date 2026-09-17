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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;

import static java.util.Comparator.comparing;

/**
 * A Multi-AZ Failover implementation of {@link ServiceInstanceListSupplier}.
 *
 * @author Jiwon Jeon
 */
public class MultiAZFailoverServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final String CACHE_KEY_FAILED = "FAILED";

	private static final String METADATA_KEY_ZONE = "zone";

	private final LoadBalancerCacheDataManager cacheDataManager;

	private final LoadBalancerZoneConfig zoneConfig;

	public MultiAZFailoverServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerCacheDataManager cacheDataManager, LoadBalancerZoneConfig zoneConfig) {
		super(delegate);
		this.cacheDataManager = cacheDataManager;
		this.zoneConfig = zoneConfig;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return getDelegate().get().map(this::filteredAndOrderedByZone);
	}

	private List<ServiceInstance> filteredAndOrderedByZone(List<ServiceInstance> serviceInstances) {
		final Set<ServiceInstance> instances = cacheDataManager.getInstance(CACHE_KEY_FAILED, Set.class);
		final List<String> zones = Stream
				.concat(Stream.of(zoneConfig.getZone()), zoneConfig.getSecondaryZones().stream()).toList();
		final Stream<ServiceInstance> instanceStream = serviceInstances.stream()
				.sorted(comparing(instance -> zones.indexOf(instance.getMetadata().get(METADATA_KEY_ZONE))));

		if (instances == null) {
			return instanceStream.toList();
		}

		return instanceStream.filter(instance -> !instances.contains(instance)).toList();
	}

}
