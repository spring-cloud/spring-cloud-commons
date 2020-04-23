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

package org.springframework.cloud.client.discovery.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * A {@link DiscoveryClient} that is composed of other discovery clients and delegates
 * calls to each of them in order.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 */
public class CompositeDiscoveryClient implements DiscoveryClient {

	private final List<DiscoveryClient> discoveryClients;

	public CompositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
		AnnotationAwareOrderComparator.sort(discoveryClients);
		this.discoveryClients = discoveryClients;
	}

	@Override
	public String description() {
		return "Composite Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
				if (instances != null && !instances.isEmpty()) {
					return instances;
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getServices() {
		LinkedHashSet<String> services = new LinkedHashSet<>();
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				List<String> serviceForClient = discoveryClient.getServices();
				if (serviceForClient != null) {
					services.addAll(serviceForClient);
				}
			}
		}
		return new ArrayList<>(services);
	}

	public List<DiscoveryClient> getDiscoveryClients() {
		return this.discoveryClients;
	}

}
