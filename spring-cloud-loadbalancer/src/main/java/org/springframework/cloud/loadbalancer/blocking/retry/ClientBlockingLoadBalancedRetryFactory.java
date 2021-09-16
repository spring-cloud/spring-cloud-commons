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

package org.springframework.cloud.loadbalancer.blocking.retry;

import org.springframework.cloud.client.loadbalancer.ClientLoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;

/**
 * An implementation of {@link LoadBalancedRetryFactory} for client specific
 * {@link BlockingLoadBalancerClient}.
 *
 * @author Andrii Bohutskyi
 */
public class ClientBlockingLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

	private final LoadBalancerProperties loadBalancerProperties;

	private final ClientLoadBalancerProperties clientLoadBalancerProperties;

	public ClientBlockingLoadBalancedRetryFactory(LoadBalancerProperties loadBalancerProperties,
			ClientLoadBalancerProperties clientLoadBalancerProperties) {
		this.loadBalancerProperties = loadBalancerProperties;
		this.clientLoadBalancerProperties = clientLoadBalancerProperties;
	}

	@Override
	public LoadBalancedRetryPolicy createRetryPolicy(String service, ServiceInstanceChooser serviceInstanceChooser) {
		final LoadBalancerProperties properties = clientLoadBalancerProperties.getClientLoadBalancerProperties(service)
				.orElse(loadBalancerProperties);

		return new BlockingLoadBalancedRetryPolicy(properties);
	}

}
