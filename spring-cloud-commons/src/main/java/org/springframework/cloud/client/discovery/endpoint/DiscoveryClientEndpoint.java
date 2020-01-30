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

package org.springframework.cloud.client.discovery.endpoint;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

@Endpoint(id = "discovery")
public class DiscoveryClientEndpoint {

	private final ReactiveDiscoveryClient discoveryClient;

	public DiscoveryClientEndpoint(ReactiveDiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	@ReadOperation
	public Mono<List<String>> services() {
		return this.discoveryClient.getServices().collectList();
	}

	@ReadOperation
	public Mono<List<ServiceInstance>> instances(@Selector String serviceId) {
		Flux<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);
		return instances.collectList();
	}

}
