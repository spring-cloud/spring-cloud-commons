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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;

/**
 * A modified {@link RoundRobinLoadBalancer} implementation that avoids picking the same
 * service instance while retrying requests.
 *
 * @author Olga Maciaszek-Sharma
 * @see RetryLoadBalancerInterceptor
 */
public class AvoidPreviousInstanceRoundRobinLoadBalancer extends RoundRobinLoadBalancer {

	private static final Log LOG = LogFactory.getLog(AvoidPreviousInstanceRoundRobinLoadBalancer.class);

	public AvoidPreviousInstanceRoundRobinLoadBalancer(
			ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
		super(serviceInstanceListSupplierProvider, serviceId);
	}

	public AvoidPreviousInstanceRoundRobinLoadBalancer(
			ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId,
			int seedPosition) {
		super(serviceInstanceListSupplierProvider, serviceId, seedPosition);
	}

	@Override
	// see original
	// https://github.com/Netflix/ocelli/blob/master/ocelli-core/
	// src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
	public Mono<Response<ServiceInstance>> choose(Request request) {
		if (!(request.getContext() instanceof RetryableRequestContext)) {
			return super.choose(request);
		}
		RetryableRequestContext context = (RetryableRequestContext) request.getContext();

		ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
				.getIfAvailable(NoopServiceInstanceListSupplier::new);
		return supplier.get().next()
				.map(instances -> getInstanceResponse(instances, context.getPreviousServiceInstance()));
	}

	Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances,
			ServiceInstance previousServiceInstance) {
		if (previousServiceInstance == null) {
			return super.getInstanceResponse(instances);
		}
		if (instances.isEmpty()) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}
		List<ServiceInstance> finalInstances = new ArrayList<>(instances);
		finalInstances.remove(previousServiceInstance);
		if (finalInstances.isEmpty()) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("No other instance available, returning previous instance: " + previousServiceInstance);
			}
			return new DefaultResponse(previousServiceInstance);
		}

		// TODO: enforce order?
		int pos = Math.abs(this.position.incrementAndGet());

		ServiceInstance instance = finalInstances.get(pos % finalInstances.size());

		return new DefaultResponse(instance);
	}

}
