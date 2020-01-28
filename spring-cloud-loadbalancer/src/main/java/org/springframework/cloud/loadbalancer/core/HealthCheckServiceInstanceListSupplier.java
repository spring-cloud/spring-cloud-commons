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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;

/**
 * @author Olga Maciaszek-Sharma
 */
public class HealthCheckServiceInstanceListSupplier
		implements ServiceInstanceListSupplier {

	private final ServiceInstanceListSupplier delegate;

	private final LoadBalancerProperties loadBalancerProperties;

	private List<ServiceInstance> instances;

	private List<ServiceInstance> healthyInstances;

	private final InstanceHealthChecker healthChecker;

	public HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties loadBalancerProperties, InstanceHealthChecker healthChecker) {
		this.delegate = delegate;
		this.loadBalancerProperties = loadBalancerProperties;
		this.healthChecker = healthChecker;
		initInstances();

	}

	private void initInstances() {
		delegate.get().subscribe(delegateInstances -> instances = delegateInstances);

		Flux<List<ServiceInstance>> healthCheckFlux = healthCheckFlux();

		healthCheckFlux
				.subscribe(verifiedInstances -> healthyInstances = verifiedInstances);
	}

	private Flux<List<ServiceInstance>> healthCheckFlux() {
		return Flux.create(emitter -> Schedulers.newSingle("Health Check Verifier", true)
				.schedulePeriodically(() -> {
					List<ServiceInstance> verifiedInstances = new ArrayList<>();
					for (ServiceInstance instance : instances) {
						if (healthCheckPassed(instance)) {
							verifiedInstances.add(instance);
						}
					}
					emitter.next(verifiedInstances);

				}, 0, 30, TimeUnit.SECONDS), FluxSink.OverflowStrategy.LATEST);
	}

	private boolean healthCheckPassed(ServiceInstance serviceInstance) {
		return healthChecker.isAlive(serviceInstance);
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return Flux.defer(() -> Flux.fromIterable(healthyInstances).collectList());
	}

}
