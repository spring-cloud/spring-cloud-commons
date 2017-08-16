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
