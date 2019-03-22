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
 * @author Spencer Gibb
 */
public class RoundRobinLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final AtomicInteger position;

	private final ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier;

	private final String serviceId;

	public RoundRobinLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier) {
		this(serviceId, serviceInstanceSupplier, new Random().nextInt(1000));
	}

	public RoundRobinLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier,
			int seedPosition) {
		this.serviceId = serviceId;
		this.serviceInstanceSupplier = serviceInstanceSupplier;
		this.position = new AtomicInteger(seedPosition);
	}

	@Override
	// see original
	// https://github.com/Netflix/ocelli/blob/master/ocelli-core/
	// src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
	public Mono<Response<ServiceInstance>> choose(Request request) {
		// TODO: move supplier to Request?
		ServiceInstanceSupplier supplier = this.serviceInstanceSupplier.getIfAvailable();
		return supplier.get().collectList().map(instances -> {
			if (instances.isEmpty()) {
				log.warn("No servers available for service: " + this.serviceId);
				return new EmptyResponse();
			}
			// TODO: enforce order?
			int pos = Math.abs(this.position.incrementAndGet());

			ServiceInstance instance = instances.get(pos % instances.size());

			return new DefaultResponse(instance);
		});
	}

}
