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

package org.springframework.cloud.client.loadbalancer.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * @author Spencer Gibb
 * TODO: rename serverlist?
 */
public class DiscoveryClientServiceInstanceSupplier implements ServiceInstanceSupplier {

	private final DiscoveryClient delegate;
	private final Environment environment;

	public DiscoveryClientServiceInstanceSupplier(DiscoveryClient delegate, Environment environment) {
		this.delegate = delegate;
		this.environment = environment;
	}

	@Cacheable(cacheNames = "discovery-client", key = "${loadbalancer.client.name}")
	public List<ServiceInstance> get() {
		return delegate.getInstances(getServiceId());
	}

	public String getServiceId() {
		return environment.getProperty("loadbalancer.client.name");
	}
}
