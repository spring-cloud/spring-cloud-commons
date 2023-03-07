/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.loadbalancer.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.loadbalancer.core.LoadBalancerCacheDataManager;
import org.springframework.web.reactive.result.view.RequestContext;

/**
 * A LoadBalancerLifecycle implementation that defines behaviors before and after
 * load-balancing, considering Multi-AZ failover.
 *
 * @author Jiwon Jeon
 */
public class MultiAZFailoverLoadBalancerLifecycle
		implements LoadBalancerLifecycle<RequestContext, ResponseData, ServiceInstance> {

	private static final Log LOG = LogFactory.getLog(MultiAZFailoverLoadBalancerLifecycle.class);

	private final LoadBalancerCacheDataManager cacheDataManager;

	public MultiAZFailoverLoadBalancerLifecycle(LoadBalancerCacheDataManager cacheDataManager) {
		this.cacheDataManager = cacheDataManager;
	}

	@Override
	public boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
		return RequestContext.class.isAssignableFrom(requestContextClass)
				&& ResponseData.class.isAssignableFrom(responseClass)
				&& ServiceInstance.class.isAssignableFrom(serverTypeClass);
	}

	@Override
	public void onStart(Request<RequestContext> request) {
	}

	@Override
	public void onStartRequest(Request<RequestContext> request, Response<ServiceInstance> lbResponse) {
	}

	@Override
	public void onComplete(CompletionContext<ResponseData, ServiceInstance, RequestContext> completionContext) {
		final CompletionContext.Status status = completionContext.status();
		final ServiceInstance instance = completionContext.getLoadBalancerResponse().getServer();

		if (CompletionContext.Status.DISCARD.equals(status)) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Any instance selected. Cache will be evicted...");
			}

			cacheDataManager.clear();
		}
		else if (CompletionContext.Status.FAILED.equals(status)) {
			if (LOG.isErrorEnabled()) {
				LOG.error(String.format("Requesting to [%s] failed", instance));
			}

			cacheDataManager.putInstance(instance);
		}
	}

}
