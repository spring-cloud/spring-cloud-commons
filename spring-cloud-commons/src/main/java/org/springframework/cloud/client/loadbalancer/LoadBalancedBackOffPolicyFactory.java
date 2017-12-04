package org.springframework.cloud.client.loadbalancer;

import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;

/**
 * Factory class to return the backoff policy.
 * @author Ryan Baxter
 */
public interface LoadBalancedBackOffPolicyFactory {

	public BackOffPolicy createBackOffPolicy(String service);

	static class NoBackOffPolicyFactory implements LoadBalancedBackOffPolicyFactory {

		@Override
		public BackOffPolicy createBackOffPolicy(String service) {
			return new NoBackOffPolicy();
		}
	}
}
