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

import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

/**
 * @author Olga Maciaszek-Sharma
 */
final class LoadBalancerTestUtils {

	private LoadBalancerTestUtils() {
		throw new UnsupportedOperationException("Cannot instantiate utility class.");
	}

	static LoadBalancerClientFactory buildLoadBalancerClientFactory(String serviceId,
			LoadBalancerProperties properties) {
		LoadBalancerClientsProperties clientsProperties = new LoadBalancerClientsProperties();
		clientsProperties.getClients().put(serviceId, properties);
		return new LoadBalancerClientFactory(clientsProperties);
	}

}
