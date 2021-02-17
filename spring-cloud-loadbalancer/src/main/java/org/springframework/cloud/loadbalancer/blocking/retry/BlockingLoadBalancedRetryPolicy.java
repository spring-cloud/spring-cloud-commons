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
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
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

	private final LoadBalancerProperties properties;

	private int sameServerCount = 0;

	private int nextServerCount = 0;

	public BlockingLoadBalancedRetryPolicy(LoadBalancerProperties properties) {
		this.properties = properties;
	}

	public boolean canRetry(LoadBalancedRetryContext context) {
		HttpMethod method = context.getRequest().getMethod();
		return HttpMethod.GET.equals(method) || properties.getRetry().isRetryOnAllOperations();
	}

	@Override
	public boolean canRetrySameServer(LoadBalancedRetryContext context) {
		return sameServerCount < properties.getRetry().getMaxRetriesOnSameServiceInstance() && canRetry(context);
	}

	@Override
	public boolean canRetryNextServer(LoadBalancedRetryContext context) {
		// After the failure, we increment first and then check, hence the equality check
		return nextServerCount <= properties.getRetry().getMaxRetriesOnNextServiceInstance() && canRetry(context);
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
				// We want the service instance to be set by
				// `RetryLoadBalancerInterceptor`
				// in order to get the entire data of the request
				context.setServiceInstance(null);
			}
		}
		else {
			sameServerCount++;
		}
	}

	@Override
	public boolean retryableStatusCode(int statusCode) {
		return properties.getRetry().getRetryableStatusCodes().contains(statusCode);
	}

}
