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

package org.springframework.cloud.client.discovery.composite.reactive;

import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.commons.publisher.CloudFlux;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * A {@link ReactiveDiscoveryClient} that is composed of other discovery clients and
 * delegates calls to each of them in order.
 *
 * @author Tim Ysewyn
 */
public class ReactiveCompositeDiscoveryClient implements ReactiveDiscoveryClient {

	private final List<ReactiveDiscoveryClient> discoveryClients;

	public ReactiveCompositeDiscoveryClient(
			List<ReactiveDiscoveryClient> discoveryClients) {
		AnnotationAwareOrderComparator.sort(discoveryClients);
		this.discoveryClients = discoveryClients;
	}

	@Override
	public String description() {
		return "Composite Reactive Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		if (discoveryClients == null || discoveryClients.isEmpty()) {
			return Flux.empty();
		}
		List<Flux<ServiceInstance>> serviceInstances = new ArrayList<>();
		for (ReactiveDiscoveryClient discoveryClient : discoveryClients) {
			serviceInstances.add(discoveryClient.getInstances(serviceId));
		}
		return CloudFlux.firstNonEmpty(serviceInstances);
	}

	@Override
	public Flux<String> getServices() {
		if (discoveryClients == null || discoveryClients.isEmpty()) {
			return Flux.empty();
		}
		return Flux.fromIterable(discoveryClients)
				.flatMap(ReactiveDiscoveryClient::getServices);
	}

	List<ReactiveDiscoveryClient> getDiscoveryClients() {
		return discoveryClients;
	}

}
