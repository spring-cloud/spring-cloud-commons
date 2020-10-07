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

package org.springframework.cloud.client.discovery.simple;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * A {@link org.springframework.cloud.client.discovery.DiscoveryClient} that will use the
 * properties file as a source of service instances.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 */
public class SimpleDiscoveryClient implements DiscoveryClient {

	private SimpleDiscoveryProperties simpleDiscoveryProperties;

	public SimpleDiscoveryClient(SimpleDiscoveryProperties simpleDiscoveryProperties) {
		this.simpleDiscoveryProperties = simpleDiscoveryProperties;
	}

	@Override
	public String description() {
		return "Simple Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		List<DefaultServiceInstance> serviceInstanceForService = this.simpleDiscoveryProperties
				.getInstances().get(serviceId);

		if (serviceInstanceForService != null) {
			serviceInstances.addAll(serviceInstanceForService);
		}
		return serviceInstances;
	}

	@Override
	public List<String> getServices() {
		return new ArrayList<>(this.simpleDiscoveryProperties.getInstances().keySet());
	}

	@Override
	public int getOrder() {
		return this.simpleDiscoveryProperties.getOrder();
	}

}
