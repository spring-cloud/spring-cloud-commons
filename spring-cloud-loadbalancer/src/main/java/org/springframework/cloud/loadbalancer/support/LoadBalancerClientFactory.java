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

package org.springframework.cloud.loadbalancer.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.core.env.Environment;

/**
 * A factory that creates client, load balancer and client configuration instances. It
 * creates a Spring ApplicationContext per client name, and extracts the beans that it
 * needs from there.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerClientFactory extends NamedContextFactory<LoadBalancerClientSpecification>
		implements ReactiveLoadBalancer.Factory<ServiceInstance> {

	private static final Log log = LogFactory.getLog(LoadBalancerClientFactory.class);

	/**
	 * Property source name for load balancer.
	 */
	public static final String NAMESPACE = "loadbalancer";

	/**
	 * Property for client name within the load balancer namespace.
	 */
	public static final String PROPERTY_NAME = NAMESPACE + ".client.name";

	private final LoadBalancerClientsProperties properties;

	/**
	 * @deprecated in favour of
	 * {@link LoadBalancerClientFactory#LoadBalancerClientFactory(LoadBalancerClientsProperties)}
	 */
	@Deprecated
	public LoadBalancerClientFactory() {
		this(null);
	}

	public LoadBalancerClientFactory(LoadBalancerClientsProperties properties) {
		super(LoadBalancerClientConfiguration.class, NAMESPACE, PROPERTY_NAME);
		this.properties = properties;
	}

	public static String getName(Environment environment) {
		return environment.getProperty(PROPERTY_NAME);
	}

	@Override
	public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
		return getInstance(serviceId, ReactorServiceInstanceLoadBalancer.class);
	}

	@Override
	public LoadBalancerProperties getProperties(String serviceId) {
		if (properties == null) {
			if (log.isWarnEnabled()) {
				log.warn("LoadBalancerClientsProperties is null. Please use the new constructor.");
			}
			return null;
		}
		if (serviceId == null || !properties.getClients().containsKey(serviceId)) {
			// no specific client properties, return default
			return properties;
		}
		// because specifics are overlayed on top of defaults, everything in `properties`,
		// unless overridden, is in `clientsProperties`
		return properties.getClients().get(serviceId);
	}

}
