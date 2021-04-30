/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

/**
 * Factory class used to provide client properties.
 *
 * @author Andrii Bohutskyi
 */
public class LoadBalancerPropertiesFactory {

	private final LoadBalancerProperties globalProperties;

	private final LoadBalancerClientProperties servicesProperties;

	private final boolean isServiceProperties;

	public LoadBalancerPropertiesFactory(LoadBalancerProperties globalProperties,
			LoadBalancerClientProperties servicesProperties, boolean isServiceProperties) {
		this.globalProperties = globalProperties;
		this.servicesProperties = servicesProperties;
		this.isServiceProperties = isServiceProperties;
	}

	public LoadBalancerProperties getLoadBalancerProperties(String serviceName) {
		return isServiceProperties ? getLoadBalancerServiceProperties(serviceName) : globalProperties;
	}

	public LoadBalancerProperties getGlobalLoadBalancerProperties() {
		return globalProperties;
	}

	private LoadBalancerProperties getLoadBalancerServiceProperties(String serviceName) {
		return servicesProperties.getClient().getOrDefault(serviceName,
				servicesProperties.getDefaultClientProperties());
	}

}
