/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer.reactive.endpoint;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.Factory;

@Endpoint(id = "loadbalancer")
public class LoadBalancerEndpoint {

	private final ObjectProvider<Factory<ServiceInstance>> clientFactory;

	public LoadBalancerEndpoint(ObjectProvider<Factory<ServiceInstance>> clientFactory) {
		this.clientFactory = clientFactory;
	}

	@ReadOperation
	public Mono<ServiceInstance> choose(@Selector String serviceId,
			@Selector String operation) {
		if (!"choose".equalsIgnoreCase(operation)) {
			return Mono.error(
					new IllegalArgumentException("Unknown operation: " + operation));
		}
		Factory<ServiceInstance> factory = this.clientFactory.getIfAvailable();
		if (factory == null) {
			return Mono.empty();
		}

		ReactiveLoadBalancer<ServiceInstance> loadBalancer = factory
				.getInstance(serviceId);
		return Mono.from(loadBalancer.choose()).flatMap(response -> {
			if (response.hasServer()) {
				return Mono.just(response.getServer());
			}
			return Mono.empty();
		});
	}

}
