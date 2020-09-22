package org.springframework.cloud.loadbalancer.blocking.retry;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;

/**
 * @author Olga Maciaszek-Sharma
 */
public class BlockingLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

	private final LoadBalancerProperties.Retry retryProperties;

	public BlockingLoadBalancedRetryFactory(LoadBalancerProperties properties) {
		retryProperties = properties.getRetry();
	}

	@Override
	public LoadBalancedRetryPolicy createRetryPolicy(String serviceId, ServiceInstanceChooser serviceInstanceChooser) {
		return new BlockingLoadBalancedRetryPolicy(serviceId, serviceInstanceChooser, retryProperties);
	}
}
