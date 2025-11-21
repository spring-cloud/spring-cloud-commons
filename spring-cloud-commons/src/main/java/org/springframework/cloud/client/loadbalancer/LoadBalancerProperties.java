/*
 * Copyright 2012-present the original author or authors.
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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;
import reactor.util.retry.RetryBackoffSpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.client.RestTemplate;

/**
 * The base configuration bean for Spring Cloud LoadBalancer.
 *
 * See {@link LoadBalancerClientsProperties} for the {@link ConfigurationProperties}
 * annotation.
 *
 * @author Olga Maciaszek-Sharma
 * @author Gandhimathi Velusamy
 * @author Zhuozhi Ji
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
	 * If this flag is set to {@code true},
	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
	 * to call {@code delegate.get(request)} in classes assignable from
	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
	 * instance supplier hierarchy directly after the supplier performing instance
	 * retrieval over the network, before any request-based filtering is done,
	 * {@code true} by default.
	 */
	private boolean callGetWithRequestOnDelegates = true;

	/**
	 * Properties for
	 * {@code org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier}.
	 */
	private Subset subset = new Subset();

	/**
	 * Enabling X-Forwarded Host and Proto Headers.
	 */
	private XForwarded xForwarded = new XForwarded();

	/**
	 * Properties for LoadBalancer metrics.
	 */
	private Stats stats = new Stats();

	/**
	 * Properties for API version-based load-balancing.
	 */
	private ApiVersion apiVersion = new ApiVersion();

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

	public void setXForwarded(XForwarded xForwarded) {
		this.xForwarded = xForwarded;
	}

	public XForwarded getXForwarded() {
		return xForwarded;
	}

	public boolean isCallGetWithRequestOnDelegates() {
		return callGetWithRequestOnDelegates;
	}

	public Subset getSubset() {
		return subset;
	}

	public void setSubset(Subset subset) {
		this.subset = subset;
	}

	public void setCallGetWithRequestOnDelegates(boolean callGetWithRequestOnDelegates) {
		this.callGetWithRequestOnDelegates = callGetWithRequestOnDelegates;
	}

	public Stats getStats() {
		return stats;
	}

	public void setStats(Stats stats) {
		this.stats = stats;
	}

	public ApiVersion getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(ApiVersion apiVersion) {
		this.apiVersion = apiVersion;
	}

	public static class StickySession {

		/**
		 * The default name of the cookie holding the preferred instance id.
		 */
		public static final String DEFAULT_INSTANCE_ID_COOKIE_NAME = "sc-lb-instance-id";

		/**
		 * The name of the cookie holding the preferred instance id.
		 */
		private String instanceIdCookieName = DEFAULT_INSTANCE_ID_COOKIE_NAME;

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
		private @Nullable Integer port;

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

		/**
		 * Indicates whether the {@code healthCheckFlux} should emit on each alive
		 * {@link ServiceInstance} that has been retrieved. If set to {@code false}, the
		 * entire alive instances sequence is first collected into a list and only then
		 * emitted.
		 */
		private boolean updateResultsList = true;

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

		public @Nullable Integer getPort() {
			return port;
		}

		public void setPort(@Nullable Integer port) {
			this.port = port;
		}

		public boolean isUpdateResultsList() {
			return updateResultsList;
		}

		public void setUpdateResultsList(boolean updateResultsList) {
			this.updateResultsList = updateResultsList;
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
		 * Indicates retries should be attempted for all exceptions, not only those
		 * specified in {@code retryableExceptions}.
		 */
		private boolean retryOnAllExceptions = false;

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
		 * A {@link Set} of {@link Throwable} classes that should trigger a retry.
		 */
		private Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>(
				Arrays.asList(IOException.class, TimeoutException.class, RetryableStatusCodeException.class,
						org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class));

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

		public Set<Class<? extends Throwable>> getRetryableExceptions() {
			return retryableExceptions;
		}

		public void setRetryableExceptions(Set<Class<? extends Throwable>> retryableExceptions) {
			retryableExceptions
				.add(org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class);
			this.retryableExceptions = retryableExceptions;
		}

		public Backoff getBackoff() {
			return backoff;
		}

		public void setBackoff(Backoff backoff) {
			this.backoff = backoff;
		}

		public boolean isRetryOnAllExceptions() {
			return retryOnAllExceptions;
		}

		public void setRetryOnAllExceptions(boolean retryOnAllExceptions) {
			this.retryOnAllExceptions = retryOnAllExceptions;
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

	public static class Subset {

		/**
		 * Instance id of deterministic subsetting. If not set,
		 * {@link IdUtils#getDefaultInstanceId(PropertyResolver)} will be used.
		 */
		private String instanceId = "";

		/**
		 * Max subset size of deterministic subsetting.
		 */
		private int size = 100;

		public String getInstanceId() {
			return instanceId;
		}

		public void setInstanceId(String instanceId) {
			this.instanceId = instanceId;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

	}

	public static class Stats {

		/**
		 * Indicates whether the {@code path} should be added to {@code uri} tag in
		 * metrics. When {@link RestTemplate} is used to execute load-balanced requests
		 * with high cardinality paths, setting it to {@code false} is recommended.
		 */
		private boolean includePath = true;

		public boolean isIncludePath() {
			return includePath;
		}

		public void setIncludePath(boolean includePath) {
			this.includePath = includePath;
		}

	}

	public static class ApiVersion {

		/**
		 * Indicates whether the API version is required with each request.
		 */
		private boolean required = false;

		/**
		 * Sets default version that should be used for each request.
		 */
		@Name("default")
		private @Nullable String defaultVersion;

		/**
		 * Uses the HTTP header with the given name to obtain the version.
		 */
		private @Nullable String header;

		/**
		 * Uses the query parameter with the given name to obtain the version.
		 */
		private @Nullable String queryParameter;

		/**
		 * Uses the path segment at the given index to obtain the version.
		 */
		private @Nullable Integer pathSegment;

		/**
		 * Uses the media type parameter with the given name to obtain the version.
		 */
		private Map<MediaType, String> mediaTypeParameters = new LinkedHashMap<>();

		/**
		 * Indicates whether all the available instances should be returned if no
		 * instances for the specified version are available.
		 */
		private boolean fallbackToAvailableInstances = false;

		public boolean getRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public @Nullable String getDefaultVersion() {
			return defaultVersion;
		}

		public void setDefaultVersion(@Nullable String defaultVersion) {
			this.defaultVersion = defaultVersion;
		}

		public @Nullable String getHeader() {
			return header;
		}

		public void setHeader(@Nullable String header) {
			this.header = header;
		}

		public @Nullable String getQueryParameter() {
			return queryParameter;
		}

		public void setQueryParameter(@Nullable String queryParameter) {
			this.queryParameter = queryParameter;
		}

		public @Nullable Integer getPathSegment() {
			return pathSegment;
		}

		public void setPathSegment(@Nullable Integer pathSegment) {
			this.pathSegment = pathSegment;
		}

		public Map<MediaType, String> getMediaTypeParameters() {
			return mediaTypeParameters;
		}

		public void setMediaTypeParameters(Map<MediaType, String> mediaTypeParameters) {
			this.mediaTypeParameters = mediaTypeParameters;
		}

		public boolean isFallbackToAvailableInstances() {
			return fallbackToAvailableInstances;
		}

		public void setFallbackToAvailableInstances(boolean fallbackToAvailableInstances) {
			this.fallbackToAvailableInstances = fallbackToAvailableInstances;
		}

	}

}
