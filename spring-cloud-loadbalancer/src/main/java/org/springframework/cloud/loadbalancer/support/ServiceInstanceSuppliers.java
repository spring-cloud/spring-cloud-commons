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

package org.springframework.cloud.loadbalancer.support;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;

/**
 * Utility class for service instances.
 *
 * @author Spencer Gibb
 */
public final class ServiceInstanceSuppliers {

	private ServiceInstanceSuppliers() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static ServiceInstanceSupplier from(String serviceId,
			ServiceInstance... instances) {
		return new ServiceInstanceSupplier() {
			@Override
			public Flux<ServiceInstance> get() {
				return Flux.just(instances);
			}

			@Override
			public String getServiceId() {
				return serviceId;
			}
		};
	}

	public static ObjectProvider<ServiceInstanceSupplier> toProvider(String serviceId,
			ServiceInstance... instances) {
		return new SimpleObjectProvider<>(from(serviceId, instances));
	}

}
