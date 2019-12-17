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

import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;

/**
 * An implementation of {@link ServiceInstanceListSupplier} that filters instances
 * retrieved by the delegate by zone. The zone is retrieved from the
 * <code>spring.cloud.loadbalancer.zone</code> property. If no instances are found for the
 * requested zone, all instances retrieved by the delegate are returned.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 */
public class ZonePreferenceServiceInstanceListSupplier
		implements ServiceInstanceListSupplier {

	private final ServiceInstanceListSupplier delegate;

	private final Environment environment;

	private String zone;

	public ZonePreferenceServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			Environment environment) {
		this.delegate = delegate;
		this.environment = environment;
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(this::filteredByZone);
	}

	private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
		if (zone == null) {
			zone = environment.getProperty("spring.cloud.loadbalancer.zone");
		}
		if (zone != null) {
			List<ServiceInstance> filteredInstances = serviceInstances.stream()
					.filter(serviceInstance -> serviceInstance.getZone() != null)
					.filter(serviceInstance -> zone
							.equalsIgnoreCase(serviceInstance.getZone()))
					.collect(Collectors.toList());
			if (filteredInstances.size() > 0) {
				return filteredInstances;
			}
		}
		return serviceInstances;
	}

}
