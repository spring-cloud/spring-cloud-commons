package org.springframework.cloud.client.loadbalancer.impl.scoped;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * @author Spencer Gibb
 */
public class CachedDiscoveryClient implements ScopedDiscoveryClient {

	private final DiscoveryClient delegate;
	private final Environment environment;

	public CachedDiscoveryClient(DiscoveryClient delegate, Environment environment) {
		this.delegate = delegate;
		this.environment = environment;
	}

	@Cacheable(cacheNames = "discovery-client", key = "${loadbalancer.client.name}")
	public List<ServiceInstance> getInstances() {
		return delegate.getInstances(getServiceId());
	}

	public String getServiceId() {
		return environment.getProperty("loadbalancer.client.name");
	}
}
