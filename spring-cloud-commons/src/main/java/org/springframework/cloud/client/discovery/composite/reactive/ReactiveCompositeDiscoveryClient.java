/*
 * Copyright 2012-present the original author or authors.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.commons.publisher.CloudFlux;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * A {@link ReactiveDiscoveryClient} that is composed of other discovery clients and
 * delegates calls to each of them. By default, instance lookup uses the fastest non-empty
 * response. Client ordering can instead be enforced by using the constructor accepting
 * {@code orderEnforced}. Instance results can be combined from all discovery clients by
 * enabling {@code combineInstances}.
 *
 * @author Tim Ysewyn
 */
public class ReactiveCompositeDiscoveryClient implements ReactiveDiscoveryClient {

	private static final Log logger = LogFactory.getLog(ReactiveCompositeDiscoveryClient.class);

	private final List<ReactiveDiscoveryClient> discoveryClients;

	private final boolean combineInstances;

	private final boolean orderEnforced;

	public ReactiveCompositeDiscoveryClient(List<ReactiveDiscoveryClient> discoveryClients) {
		this(discoveryClients, false, false);
	}

	public ReactiveCompositeDiscoveryClient(List<ReactiveDiscoveryClient> discoveryClients, boolean orderEnforced) {
		this(discoveryClients, orderEnforced, false);
	}

	/**
	 * Create a composite discovery client.
	 * @param discoveryClients the discovery clients to delegate to
	 * @param orderEnforced whether to wait for each client in order before falling back
	 * to the next client if it returns no instances
	 * @param combineInstances whether to combine instances returned by all discovery
	 * clients
	 */
	public ReactiveCompositeDiscoveryClient(List<ReactiveDiscoveryClient> discoveryClients, boolean orderEnforced,
			boolean combineInstances) {
		AnnotationAwareOrderComparator.sort(discoveryClients);
		this.discoveryClients = discoveryClients;
		this.orderEnforced = orderEnforced;
		this.combineInstances = combineInstances;
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

		if (combineInstances) {
			List<Flux<ServiceInstance>> instances = discoveryClients.stream()
				.map(client -> getInstancesFromClient(client, serviceId))
				.toList();

			return orderEnforced ? Flux.concat(instances) : Flux.mergeSequential(instances);
		}

		if (!orderEnforced) {
			return CloudFlux
				.firstNonEmpty(discoveryClients.stream().map(client -> client.getInstances(serviceId)).toList());
		}

		Flux<ServiceInstance> serviceInstances = Flux.empty();
		for (ReactiveDiscoveryClient discoveryClient : discoveryClients) {
			serviceInstances = serviceInstances.switchIfEmpty(discoveryClient.getInstances(serviceId));
		}
		return serviceInstances;
	}

	private Flux<ServiceInstance> getInstancesFromClient(ReactiveDiscoveryClient client, String serviceId) {
		return Flux.defer(() -> client.getInstances(serviceId)).onErrorResume(error -> {
			logger.warn("Error retrieving instances from " + client.description() + " for service " + serviceId, error);
			return Flux.empty();
		});
	}

	@Override
	public Flux<String> getServices() {
		if (discoveryClients == null || discoveryClients.isEmpty()) {
			return Flux.empty();
		}
		return Flux.fromIterable(discoveryClients).flatMap(ReactiveDiscoveryClient::getServices);
	}

	public List<ReactiveDiscoveryClient> getDiscoveryClients() {
		return discoveryClients;
	}

}
