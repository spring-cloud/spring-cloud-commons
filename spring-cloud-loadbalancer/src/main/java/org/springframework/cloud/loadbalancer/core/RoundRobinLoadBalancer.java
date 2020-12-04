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

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;

/**
 * A Round-Robin-based implementation of {@link ReactorServiceInstanceLoadBalancer}.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final AtomicInteger position;

	@Deprecated
	private ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier;

	private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

	private final String serviceId;

	/**
	 * @param serviceId id of the service for which to choose an instance
	 * @param serviceInstanceSupplier a provider of {@link ServiceInstanceSupplier} that
	 * will be used to get available instances
	 * @deprecated Use {@link #RoundRobinLoadBalancer(ObjectProvider, String)}} instead.
	 */
	@Deprecated
	public RoundRobinLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier) {
		this(serviceId, serviceInstanceSupplier, new Random().nextInt(1000));
	}

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public RoundRobinLoadBalancer(
			ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId) {
		this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
	}

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 * @param seedPosition Round Robin element position marker
	 */
	public RoundRobinLoadBalancer(
			ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId, int seedPosition) {
		this.serviceId = serviceId;
		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
		this.position = new AtomicInteger(seedPosition);
	}

	/**
	 * @param serviceId id of the service for which to choose an instance
	 * @param serviceInstanceSupplier a provider of {@link ServiceInstanceSupplier} that
	 * will be used to get available instances
	 * @param seedPosition Round Robin element position marker
	 * @deprecated Use {@link #RoundRobinLoadBalancer(ObjectProvider, String, int)}}
	 * instead.
	 */
	@Deprecated
	public RoundRobinLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier,
			int seedPosition) {
		this.serviceId = serviceId;
		this.serviceInstanceSupplier = serviceInstanceSupplier;
		this.position = new AtomicInteger(seedPosition);
	}

	@SuppressWarnings("rawtypes")
	@Override
	// see original
	// https://github.com/Netflix/ocelli/blob/master/ocelli-core/
	// src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
	public Mono<Response<ServiceInstance>> choose(Request request) {
		// TODO: move supplier to Request?
		// Temporary conditional logic till deprecated members are removed.
		if (serviceInstanceListSupplierProvider != null) {
			ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
					.getIfAvailable(NoopServiceInstanceListSupplier::new);
			return supplier.get().next()
					.map(serviceInstances -> processInstanceResponse(supplier,
							serviceInstances));
		}
		ServiceInstanceSupplier supplier = this.serviceInstanceSupplier
				.getIfAvailable(NoopServiceInstanceSupplier::new);
		return supplier.get().collectList().map(this::getInstanceResponse);
	}

	private Response<ServiceInstance> processInstanceResponse(
			ServiceInstanceListSupplier supplier,
			List<ServiceInstance> serviceInstances) {
		Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(
				serviceInstances);
		if (supplier instanceof SelectedInstanceCallback
				&& serviceInstanceResponse.hasServer()) {
			((SelectedInstanceCallback) supplier)
					.selectedServiceInstance(serviceInstanceResponse.getServer());
		}
		return serviceInstanceResponse;
	}

	private Response<ServiceInstance> getInstanceResponse(
			List<ServiceInstance> instances) {
		if (instances.isEmpty()) {
			log.warn("No servers available for service: " + this.serviceId);
			return new EmptyResponse();
		}
		// TODO: enforce order?
		int pos = Math.abs(this.position.incrementAndGet());

		ServiceInstance instance = instances.get(pos % instances.size());

		return new DefaultResponse(instance);
	}

}
