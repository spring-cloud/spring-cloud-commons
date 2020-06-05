package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
public class EmptyResponse implements Response<ServiceInstance> {

	@Override
	public boolean hasServer() {
		return false;
	}

	@Override
	public ServiceInstance getServer() {
		return null;
	}

	@Override
	public void onComplete(CompletionContext completionContext) {
		// TODO: implement
	}

}
