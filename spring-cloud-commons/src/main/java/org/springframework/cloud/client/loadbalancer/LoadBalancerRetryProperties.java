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

package org.springframework.cloud.client.loadbalancer;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

/**
 * Configuration properties for the {@link LoadBalancerClient}.
 *
 * @author Ryan Baxter
 */
@ConfigurationProperties("spring.cloud.loadbalancer.retry")
public class LoadBalancerRetryProperties {

	private boolean enabled = true;

	/**
	 * Indicates retries should be attempted on operations other than
	 * {@link HttpMethod#GET}.
	 */
	private boolean retryOnAllOperations = false;

	/**
	 * Number of retries to be executed on the same <code>ServiceInstance</code>.
	 */
	private int maxRetriesOnSameServiceInstance = 0;

	/**
	 * Number of retries to be executed on the next <code>ServiceInstance</code>. A
	 * <code>ServiceInstance</code> is chosen before each retry call.
	 */
	private int maxRetriesOnNextServiceInstance = 1;

	/**
	 * A {@link Set} of status codes that should trigger a retry.
	 */
	private Set<Integer> retryableStatusCodes = new HashSet<>();

	/**
	 * Returns true if the load balancer should retry failed requests.
	 * @return True if the load balancer should retry failed requests; false otherwise.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Sets whether the load balancer should retry failed requests.
	 * @param enabled Whether the load balancer should retry failed requests.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRetryOnAllOperations() {
		return retryOnAllOperations;
	}

	public void setRetryOnAllOperations(boolean retryOnAllOperations) {
		this.retryOnAllOperations = retryOnAllOperations;
	}

	public int getMaxRetriesOnSameServiceInstance() {
		return maxRetriesOnSameServiceInstance;
	}

	public void setMaxRetriesOnSameServiceInstance(int maxRetriesOnSameServiceInstance) {
		this.maxRetriesOnSameServiceInstance = maxRetriesOnSameServiceInstance;
	}

	public int getMaxRetriesOnNextServiceInstance() {
		return maxRetriesOnNextServiceInstance;
	}

	public void setMaxRetriesOnNextServiceInstance(int maxRetriesOnNextServiceInstance) {
		this.maxRetriesOnNextServiceInstance = maxRetriesOnNextServiceInstance;
	}

	public Set<Integer> getRetryableStatusCodes() {
		return retryableStatusCodes;
	}

	public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) {
		this.retryableStatusCodes = retryableStatusCodes;
	}

}
