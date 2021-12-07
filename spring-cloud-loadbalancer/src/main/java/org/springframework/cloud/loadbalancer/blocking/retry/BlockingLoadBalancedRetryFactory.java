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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;

/**
 * An implementation of {@link LoadBalancedRetryFactory} for
 * {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.6
 */
public class BlockingLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	public BlockingLoadBalancedRetryFactory(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
		this.loadBalancerFactory = loadBalancerFactory;
	}

	@Override
	public LoadBalancedRetryPolicy createRetryPolicy(String serviceId, ServiceInstanceChooser serviceInstanceChooser) {
		return new BlockingLoadBalancedRetryPolicy(loadBalancerFactory.getProperties(serviceId));
	}

}
