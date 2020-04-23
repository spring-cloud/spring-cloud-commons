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

package org.springframework.cloud.loadbalancer.core;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * @deprecated Use {@link DiscoveryClientServiceInstanceListSupplier} instead.
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
@Deprecated
public class DiscoveryClientServiceInstanceSupplier implements ServiceInstanceSupplier {

	private final String serviceId;

	private final Flux<ServiceInstance> serviceInstances;

	public DiscoveryClientServiceInstanceSupplier(DiscoveryClient delegate,
			Environment environment) {
		this.serviceId = environment.getProperty(PROPERTY_NAME);
		this.serviceInstances = Flux
				.defer(() -> Flux.fromIterable(delegate.getInstances(serviceId)))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public DiscoveryClientServiceInstanceSupplier(ReactiveDiscoveryClient delegate,
			Environment environment) {
		this.serviceId = environment.getProperty(PROPERTY_NAME);
		this.serviceInstances = delegate.getInstances(serviceId);
	}

	@Override
	public Flux<ServiceInstance> get() {
		return this.serviceInstances;
	}

	public String getServiceId() {
		return this.serviceId;
	}

}
