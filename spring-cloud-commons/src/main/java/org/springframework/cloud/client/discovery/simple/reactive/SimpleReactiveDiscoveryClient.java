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

package org.springframework.cloud.client.discovery.simple.reactive;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

/**
 * A {@link ReactiveDiscoveryClient} that will use the properties file as a source of
 * service instances.
 *
 * @author Tim Ysewyn
 */
public class SimpleReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	private SimpleReactiveDiscoveryProperties simpleDiscoveryProperties;

	public SimpleReactiveDiscoveryClient(SimpleReactiveDiscoveryProperties simpleDiscoveryProperties) {
		this.simpleDiscoveryProperties = simpleDiscoveryProperties;
	}

	@Override
	public String description() {
		return "Simple Reactive Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return this.simpleDiscoveryProperties.getInstances(serviceId);
	}

	@Override
	public Flux<String> getServices() {
		return Flux.fromIterable(this.simpleDiscoveryProperties.getInstances().keySet());
	}

	@Override
	public int getOrder() {
		return this.simpleDiscoveryProperties.getOrder();
	}

}
