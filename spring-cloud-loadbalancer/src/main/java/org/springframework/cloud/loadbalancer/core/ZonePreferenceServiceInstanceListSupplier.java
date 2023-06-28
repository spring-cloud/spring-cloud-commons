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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;

/**
 * An implementation of {@link ServiceInstanceListSupplier} that filters instances
 * retrieved by the delegate by zone. The zone is retrieved from the
 * <code>spring.cloud.loadbalancer.zone</code> property. If the zone is not set or no
 * instances are found for the requested zone, all instances retrieved by the delegate are
 * returned.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 */
public class ZonePreferenceServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private final String ZONE = "zone";

	private final LoadBalancerZoneConfig zoneConfig;

	private String zone;

	private boolean callGetWithRequestOnDelegates;

	public ZonePreferenceServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerZoneConfig zoneConfig) {
		super(delegate);
		this.zoneConfig = zoneConfig;
	}

	public ZonePreferenceServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerZoneConfig zoneConfig,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		super(delegate);
		this.zoneConfig = zoneConfig;
		callGetWithRequestOnDelegates = loadBalancerClientFactory.getProperties(getServiceId())
				.isCallGetWithRequestOnDelegates();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return getDelegate().get().map(this::filteredByZone);
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		if (callGetWithRequestOnDelegates) {
			return getDelegate().get(request).map(this::filteredByZone);
		}
		return get();
	}

	private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
		if (zone == null) {
			zone = zoneConfig.getZone();
		}
		if (zone != null) {
			List<ServiceInstance> filteredInstances = new ArrayList<>();
			for (ServiceInstance serviceInstance : serviceInstances) {
				String instanceZone = getZone(serviceInstance);
				if (zone.equalsIgnoreCase(instanceZone)) {
					filteredInstances.add(serviceInstance);
				}
			}
			if (filteredInstances.size() > 0) {
				return filteredInstances;
			}
		}
		// If the zone is not set or there are no zone-specific instances available,
		// we return all instances retrieved for given service id.
		return serviceInstances;
	}

	private String getZone(ServiceInstance serviceInstance) {
		Map<String, String> metadata = serviceInstance.getMetadata();
		if (metadata != null) {
			return metadata.get(ZONE);
		}
		return null;
	}

}
