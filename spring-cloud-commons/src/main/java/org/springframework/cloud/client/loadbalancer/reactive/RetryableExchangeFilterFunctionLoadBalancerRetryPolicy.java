package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.http.HttpMethod;

/**
 * @author Olga Maciaszek-Sharma
 */
public class RetryableExchangeFilterFunctionLoadBalancerRetryPolicy implements LoadBalancerRetryPolicy {


	private final LoadBalancerProperties properties;

	public RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(LoadBalancerProperties properties) {
		this.properties = properties;
	}

	@Override
	public boolean canRetrySameServiceInstance(LoadBalancerRetryContext context) {
		return context.getRetriesSameServiceInstance() < properties.getRetry()
				.getMaxRetriesOnSameServiceInstance();
	}

	@Override
	public boolean canRetryNextServiceInstance(LoadBalancerRetryContext context) {
		return context.getRetriesNextServiceInstance() < properties.getRetry()
				.getMaxRetriesOnNextServiceInstance();
	}

	@Override
	public boolean retryableStatusCode(int statusCode) {
		return properties.getRetry().getRetryableStatusCodes().contains(statusCode);
	}

	@Override
	public boolean canRetryOnMethod(HttpMethod method) {
		return HttpMethod.GET.equals(method) || properties.getRetry()
				.isRetryOnAllOperations();
	}
}
