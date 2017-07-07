package org.springframework.cloud.client.loadbalancer.impl.scoped;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @author Spencer Gibb
 */
public interface ScopedDiscoveryClient {
	List<ServiceInstance> getInstances();
}
