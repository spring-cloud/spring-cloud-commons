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
 * The base configuration bean for Spring Cloud LoadBalancer.
 *
 * See {@link LoadBalancerClientsProperties} for the {@link ConfigurationProperties}
 * annotation.
 *
 * @author Olga Maciaszek-Sharma
 * @author Gandhimathi Velusamy
 * @since 2.2.1
 */
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
	 * Allows setting the name of the header used for passing the hint for hint-based
	 * service instance filtering.
	 */
	private String hintHeaderName = "X-SC-LB-Hint";

	/**
	 * Properties for Spring-Retry and Reactor Retry support in Spring Cloud LoadBalancer.
	 */
	private Retry retry = new Retry();

	/**
	 * Properties for LoadBalancer sticky-session.
	 */
	private StickySession stickySession = new StickySession();

	/**
	 * Indicates that raw status codes should be used in {@link ResponseData}.
	 */
	private boolean useRawStatusCodeInResponseData;

	/**
	 * If this flag is set to {@code true},
	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
	 * to call {@code delegate.get(request)} in classes assignable from
	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
	 * instance supplier hierarchy directly after the supplier performing instance
	 * retrieval over the network, before any request-based filtering is done. Note: in
	 * 4.1, this behaviour will become the default
	 */
	private boolean callGetWithRequestOnDelegates;

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

	public String getHintHeaderName() {
		return hintHeaderName;
	}

	public void setHintHeaderName(String hintHeaderName) {
		this.hintHeaderName = hintHeaderName;
	}

	/**
	 * Enabling X-Forwarded Host and Proto Headers.
	 */
	private XForwarded xForwarded = new XForwarded();

	public void setxForwarded(XForwarded xForwarded) {
		this.xForwarded = xForwarded;
	}

	public XForwarded getXForwarded() {
		return xForwarded;
	}

	public boolean isUseRawStatusCodeInResponseData() {
		return useRawStatusCodeInResponseData;
	}

	public void setUseRawStatusCodeInResponseData(boolean useRawStatusCodeInResponseData) {
		this.useRawStatusCodeInResponseData = useRawStatusCodeInResponseData;
	}

	/**
	 * If this flag is set to {@code true},
	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
	 * to call {@code delegate.get(request)} in classes assignable from
	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
	 * instance supplier hierarchy directly after the supplier performing instance
	 * retrieval over the network, before any request-based filtering is done. Note: in
	 * 4.1, this behaviour will become the default
	 */
	public boolean isCallGetWithRequestOnDelegates() {
		return callGetWithRequestOnDelegates;
	}

	/**
	 * If this flag is set to {@code true},
	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
	 * to call {@code delegate.get(request)} in classes assignable from
	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
	 * instance supplier hierarchy directly after the supplier performing instance
	 * retrieval over the network, before any request-based filtering is done. Note: in
	 * 4.1, this behaviour will become the default
	 */
	public void setCallGetWithRequestOnDelegates(boolean callGetWithRequestOnDelegates) {
		this.callGetWithRequestOnDelegates = callGetWithRequestOnDelegates;
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

	public static class XForwarded {

		/**
		 * To Enable X-Forwarded Headers.
		 */
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
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

		/**
		 * Path at which the health-check request should be made. Can be set up per
		 * <code>serviceId</code>. A <code>default</code> value can be set up as well. If
		 * none is set up, <code>/actuator/health</code> will be used.
		 */
		private Map<String, String> path = new LinkedCaseInsensitiveMap<>();

		/**
		 * Port at which the health-check request should be made. If none is set, the port
		 * under which the requested service is available at the service instance.
		 */
		private Integer port;

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

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
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
