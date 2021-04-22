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

package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerPropertiesFactory;
import org.springframework.http.HttpMethod;

/**
 * The default implementation of {@link LoadBalancerRetryPolicy}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @since 3.0.0
 */
public class RetryableExchangeFilterFunctionLoadBalancerRetryPolicy implements LoadBalancerRetryPolicy {

	private final LoadBalancerProperties properties;

	private LoadBalancerPropertiesFactory propertiesFactory;

	/**
	 * @deprecated Deprecated in favor of
	 * {@link #RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(LoadBalancerProperties, LoadBalancerPropertiesFactory)}
	 */
	@Deprecated
	public RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(LoadBalancerProperties properties) {
		this.properties = properties;
	}

	public RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(LoadBalancerProperties properties,
			LoadBalancerPropertiesFactory propertiesFactory) {
		this.properties = properties;
		this.propertiesFactory = propertiesFactory;
	}

	@Override
	public boolean canRetrySameServiceInstance(LoadBalancerRetryContext context) {
		return context.getRetriesSameServiceInstance() < properties.getRetry().getMaxRetriesOnSameServiceInstance();
	}

	@Override
	public boolean canRetrySameServiceInstance(String serviceId, LoadBalancerRetryContext context) {
		return context.getRetriesSameServiceInstance() < getLoadBalancerProperties(serviceId).getRetry()
				.getMaxRetriesOnSameServiceInstance();
	}

	@Override
	public boolean canRetryNextServiceInstance(LoadBalancerRetryContext context) {
		return context.getRetriesNextServiceInstance() < properties.getRetry().getMaxRetriesOnNextServiceInstance();
	}

	@Override
	public boolean canRetryNextServiceInstance(String serviceId, LoadBalancerRetryContext context) {
		return context.getRetriesNextServiceInstance() < getLoadBalancerProperties(serviceId).getRetry()
				.getMaxRetriesOnNextServiceInstance();

	}

	@Override
	public boolean retryableStatusCode(int statusCode) {
		return properties.getRetry().getRetryableStatusCodes().contains(statusCode);
	}

	@Override
	public boolean retryableStatusCode(String serviceId, int statusCode) {
		return getLoadBalancerProperties(serviceId).getRetry().getRetryableStatusCodes().contains(statusCode);
	}

	@Override
	public boolean canRetryOnMethod(HttpMethod method) {
		return HttpMethod.GET.equals(method) || properties.getRetry().isRetryOnAllOperations();
	}

	@Override
	public boolean canRetryOnMethod(String serviceId, HttpMethod method) {
		return HttpMethod.GET.equals(method)
				|| getLoadBalancerProperties(serviceId).getRetry().isRetryOnAllOperations();
	}

	@Deprecated
	private LoadBalancerProperties getLoadBalancerProperties(String serviceId) {
		if (propertiesFactory != null) {
			return propertiesFactory.getLoadBalancerProperties(serviceId);
		}
		else {
			return properties;
		}
	}

}
