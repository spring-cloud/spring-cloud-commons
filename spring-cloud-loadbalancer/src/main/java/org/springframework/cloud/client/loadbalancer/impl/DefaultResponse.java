package org.springframework.cloud.client.loadbalancer.impl;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
class DefaultResponse implements LoadBalancer.Response<ServiceInstance> {

	private final ServiceInstance serviceInstance;

	public DefaultResponse(ServiceInstance serviceInstance) {
		this.serviceInstance = serviceInstance;
	}

	@Override
	public boolean hasServer() {
		return serviceInstance != null;
	}

	@Override
	public ServiceInstance getServer() {
		return this.serviceInstance;
	}

	@Override
	public void onComplete(OnComplete onComplete) {
		//TODO: implement
	}
}
