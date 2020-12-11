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
import java.util.concurrent.ThreadLocalRandom;

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
 * A random-based implementation of {@link ReactorServiceInstanceLoadBalancer}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
public class RandomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RandomLoadBalancer.class);

	private final String serviceId;

	@Deprecated
	private ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier;

	private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public RandomLoadBalancer(
			ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId) {
		this.serviceId = serviceId;
		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
	}

	/**
	 * @param serviceId id of the service for which to choose an instance
	 * @param serviceInstanceSupplier a provider of {@link ServiceInstanceSupplier} that
	 * will be used to get available instances
	 * @deprecated Use {@link #RandomLoadBalancer(ObjectProvider, String)}} instead.
	 */
	@Deprecated
	public RandomLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier) {
		this.serviceId = serviceId;
		this.serviceInstanceSupplier = serviceInstanceSupplier;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Mono<org.springframework.cloud.client.loadbalancer.reactive.Response<ServiceInstance>> choose(
			Request request) {
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

	private org.springframework.cloud.client.loadbalancer.reactive.Response<ServiceInstance> processInstanceResponse(
			ServiceInstanceListSupplier supplier,
			List<ServiceInstance> serviceInstances) {
		org.springframework.cloud.client.loadbalancer.reactive.Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(
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
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}
		int index = ThreadLocalRandom.current().nextInt(instances.size());

		ServiceInstance instance = instances.get(index);

		return new DefaultResponse(instance);
	}

}
