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

package org.springframework.cloud.loadbalancer.blocking.retry;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerPropertiesFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;

/**
 * An implementation of {@link LoadBalancedRetryFactory} for
 * {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @since 2.2.6
 */
public class BlockingLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

	private final LoadBalancerProperties loadBalancerProperties;

	private LoadBalancerPropertiesFactory propertiesFactory;

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #BlockingLoadBalancedRetryFactory(LoadBalancerProperties, LoadBalancerPropertiesFactory)}
	 */
	@Deprecated
	public BlockingLoadBalancedRetryFactory(LoadBalancerProperties loadBalancerProperties) {
		this.loadBalancerProperties = loadBalancerProperties;
	}

	public BlockingLoadBalancedRetryFactory(LoadBalancerProperties loadBalancerProperties,
											LoadBalancerPropertiesFactory propertiesFactory) {
		this.loadBalancerProperties = loadBalancerProperties;
		this.propertiesFactory = propertiesFactory;
	}

	@Override
	public LoadBalancedRetryPolicy createRetryPolicy(String serviceId, ServiceInstanceChooser serviceInstanceChooser) {
		return new BlockingLoadBalancedRetryPolicy(getLoadBalancerProperties(serviceId));
	}

	@Deprecated
	private LoadBalancerProperties getLoadBalancerProperties(String serviceId) {
		if (propertiesFactory != null) {
			return propertiesFactory.getLoadBalancerProperties(serviceId);
		} else {
			return loadBalancerProperties;
		}
	}

}
