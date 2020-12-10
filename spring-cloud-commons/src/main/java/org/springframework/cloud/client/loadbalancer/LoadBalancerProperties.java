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

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import reactor.util.retry.RetryBackoffSpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * A {@link ConfigurationProperties} bean for Spring Cloud LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 */
@ConfigurationProperties("spring.cloud.loadbalancer")
public class LoadBalancerProperties {

	/**
	 * Properties for <code>HealthCheckServiceInstanceListSupplier</code>.
	 */
	private HealthCheck healthCheck = new HealthCheck();

	/**
	 * Allows setting the value of <code>hint</code> that is passed on to the LoadBalancer
	 * request and can subsequently be used in {@link ReactiveLoadBalancer}
	 * implementations.
	 */
	private Map<String, String> hint = new LinkedCaseInsensitiveMap<>();

	/**
	 * Properties for Spring-Retry and Reactor Retry support in Spring Cloud LoadBalancer.
	 */
	private Retry retry = new Retry();

	/**
	 * Properties for LoadBalancer sticky-session.
	 */
	private StickySession stickySession = new StickySession();

	public HealthCheck getHealthCheck() {
		return healthCheck;
	}

	public void setHealthCheck(HealthCheck healthCheck) {
		this.healthCheck = healthCheck;
	}

	public Map<String, String> getHint() {
		return hint;
	}

	public void setHint(Map<String, String> hint) {
		this.hint = hint;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	public StickySession getStickySession() {
		return stickySession;
	}

	public void setStickySession(StickySession stickySession) {
		this.stickySession = stickySession;
	}

	public static class StickySession {

		/**
		 * The name of the cookie holding the preferred instance id.
		 */
		private String instanceIdCookieName = "sc-lb-instance-id";

		/**
		 * Indicates whether a cookie with the newly selected instance should be added by
		 * SC LoadBalancer.
		 */
		private boolean addServiceInstanceCookie = false;

		public String getInstanceIdCookieName() {
			return instanceIdCookieName;
		}

		public void setInstanceIdCookieName(String instanceIdCookieName) {
			this.instanceIdCookieName = instanceIdCookieName;
		}

		public boolean isAddServiceInstanceCookie() {
			return addServiceInstanceCookie;
		}

		public void setAddServiceInstanceCookie(boolean addServiceInstanceCookie) {
			this.addServiceInstanceCookie = addServiceInstanceCookie;
		}

	}

	public static class HealthCheck {

		/**
		 * Initial delay value for the HealthCheck scheduler.
		 */
		private Duration initialDelay = Duration.ZERO;

		/**
		 * Interval for rerunning the HealthCheck scheduler.
		 */
		private Duration interval = Duration.ofSeconds(25);

		/**
		 * Interval for refetching available service instances.
		 */
		private Duration refetchInstancesInterval = Duration.ofSeconds(25);

		private Map<String, String> path = new LinkedCaseInsensitiveMap<>();

		/**
		 * Indicates whether the instances should be refetched by the
		 * <code>HealthCheckServiceInstanceListSupplier</code>. This can be used if the
		 * instances can be updated and the underlying delegate does not provide an
		 * ongoing flux.
		 */
		private boolean refetchInstances = false;

		/**
		 * Indicates whether health checks should keep repeating. It might be useful to
		 * set it to <code>false</code> if periodically refetching the instances, as every
		 * refetch will also trigger a healthcheck.
		 */
		private boolean repeatHealthCheck = true;

		public boolean getRefetchInstances() {
			return refetchInstances;
		}

		public void setRefetchInstances(boolean refetchInstances) {
			this.refetchInstances = refetchInstances;
		}

		public boolean getRepeatHealthCheck() {
			return repeatHealthCheck;
		}

		public void setRepeatHealthCheck(boolean repeatHealthCheck) {
			this.repeatHealthCheck = repeatHealthCheck;
		}

		public Duration getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(Duration initialDelay) {
			this.initialDelay = initialDelay;
		}

		public Duration getRefetchInstancesInterval() {
			return refetchInstancesInterval;
		}

		public void setRefetchInstancesInterval(Duration refetchInstancesInterval) {
			this.refetchInstancesInterval = refetchInstancesInterval;
		}

		public Map<String, String> getPath() {
			return path;
		}

		public void setPath(Map<String, String> path) {
			this.path = path;
		}

		public Duration getInterval() {
			return interval;
		}

		public void setInterval(Duration interval) {
			this.interval = interval;
		}

	}

	public static class Retry {

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
		 * Properties for Reactor Retry backoffs in Spring Cloud LoadBalancer.
		 */
		private Backoff backoff = new Backoff();

		/**
		 * Returns true if the load balancer should retry failed requests.
		 * @return True if the load balancer should retry failed requests; false
		 * otherwise.
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

		public Backoff getBackoff() {
			return backoff;
		}

		public void setBackoff(Backoff backoff) {
			this.backoff = backoff;
		}

		public static class Backoff {

			/**
			 * Indicates whether Reactor Retry backoffs should be applied.
			 */
			private boolean enabled = false;

			/**
			 * Used to set {@link RetryBackoffSpec#minBackoff}.
			 */
			private Duration minBackoff = Duration.ofMillis(5);

			/**
			 * Used to set {@link RetryBackoffSpec#maxBackoff}.
			 */
			private Duration maxBackoff = Duration.ofMillis(Long.MAX_VALUE);

			/**
			 * Used to set {@link RetryBackoffSpec#jitter}.
			 */
			private double jitter = 0.5d;

			public Duration getMinBackoff() {
				return minBackoff;
			}

			public void setMinBackoff(Duration minBackoff) {
				this.minBackoff = minBackoff;
			}

			public Duration getMaxBackoff() {
				return maxBackoff;
			}

			public void setMaxBackoff(Duration maxBackoff) {
				this.maxBackoff = maxBackoff;
			}

			public double getJitter() {
				return jitter;
			}

			public void setJitter(double jitter) {
				this.jitter = jitter;
			}

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

	}

}
