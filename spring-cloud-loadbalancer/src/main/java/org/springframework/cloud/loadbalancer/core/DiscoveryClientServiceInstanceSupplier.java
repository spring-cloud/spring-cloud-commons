/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * @author Spencer Gibb
 */
//TODO: how to eagerly invoke cache, such that the first client use is already cached
public class DiscoveryClientServiceInstanceSupplier implements ServiceInstanceSupplier {

	private final DiscoveryClient delegate;
	private final Environment environment;
	private final CacheManager cacheManager;

	public DiscoveryClientServiceInstanceSupplier(DiscoveryClient delegate, Environment environment, CacheManager cacheManager) {
		this.delegate = delegate;
		this.environment = environment;
		this.cacheManager = cacheManager;
	}

	// @Cacheable(cacheNames = "discovery-client-service-instances", key = "${loadbalancer.client.name}")
	public List<ServiceInstance> get() {
		Cache cache = this.cacheManager.getCache("discovery-client-service-instances");
		String serviceId = getServiceId();
		return cache.get(serviceId, () -> delegate.getInstances(serviceId));
		// return delegate.getInstances(getServiceId());
	}

	public String getServiceId() {
		return environment.getProperty(PROPERTY_NAME);
	}
}
