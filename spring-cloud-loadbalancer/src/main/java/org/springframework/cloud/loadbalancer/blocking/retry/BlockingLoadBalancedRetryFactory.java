package org.springframework.cloud.loadbalancer.blocking.retry;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;

/**
 * An implementation of {@link LoadBalancedRetryFactory} for {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 */
public class BlockingLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

	private final LoadBalancerRetryProperties retryProperties;

	public BlockingLoadBalancedRetryFactory(LoadBalancerRetryProperties retryProperties) {
		this.retryProperties = retryProperties;
	}

	@Override
	public LoadBalancedRetryPolicy createRetryPolicy(String serviceId, ServiceInstanceChooser serviceInstanceChooser) {
		return new BlockingLoadBalancedRetryPolicy(serviceId, serviceInstanceChooser, retryProperties);
	}
}
