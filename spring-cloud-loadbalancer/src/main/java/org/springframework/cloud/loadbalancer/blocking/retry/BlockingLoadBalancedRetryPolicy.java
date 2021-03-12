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

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.http.HttpMethod;

/**
 * A {@link LoadBalancedRetryPolicy} implementation for
 * {@link BlockingLoadBalancerClient}. Based on <code>RibbonLoadBalancedRetryPolicy</code>
 * to achieve feature-parity.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.6
 */
public class BlockingLoadBalancedRetryPolicy implements LoadBalancedRetryPolicy {

	private final LoadBalancerRetryProperties retryProperties;

	private final ServiceInstanceChooser serviceInstanceChooser;

	private final String serviceId;

	private int sameServerCount = 0;

	private int nextServerCount = 0;

	public BlockingLoadBalancedRetryPolicy(String serviceId,
			ServiceInstanceChooser serviceInstanceChooser,
			LoadBalancerRetryProperties retryProperties) {
		this.serviceId = serviceId;
		this.serviceInstanceChooser = serviceInstanceChooser;
		this.retryProperties = retryProperties;
	}

	public boolean canRetry(LoadBalancedRetryContext context) {
		HttpMethod method = context.getRequest().getMethod();
		return HttpMethod.GET.equals(method) || retryProperties.isRetryOnAllOperations();
	}

	@Override
	public boolean canRetrySameServer(LoadBalancedRetryContext context) {
		return sameServerCount < retryProperties.getMaxRetriesOnSameServiceInstance()
				&& canRetry(context);
	}

	@Override
	public boolean canRetryNextServer(LoadBalancedRetryContext context) {
		// After the failure, we increment first and then check, hence the equality check
		return nextServerCount <= retryProperties.getMaxRetriesOnNextServiceInstance()
				&& canRetry(context);
	}

	@Override
	public void close(LoadBalancedRetryContext context) {

	}

	@Override
	public void registerThrowable(LoadBalancedRetryContext context, Throwable throwable) {
		if (!canRetrySameServer(context) && canRetry(context)) {
			// Reset same server since we are moving to a new ServiceInstance
			sameServerCount = 0;
			nextServerCount++;
			if (!canRetryNextServer(context)) {
				context.setExhaustedOnly();
			}
			else {
				context.setServiceInstance(serviceInstanceChooser.choose(serviceId));
			}
		}
		else {
			sameServerCount++;
		}
	}

	@Override
	public boolean retryableStatusCode(int statusCode) {
		return retryProperties.getRetryableStatusCodes().contains(statusCode);
	}

}
