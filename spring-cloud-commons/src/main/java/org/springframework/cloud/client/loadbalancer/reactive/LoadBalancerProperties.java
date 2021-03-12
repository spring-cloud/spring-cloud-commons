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

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
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

	public HealthCheck getHealthCheck() {
		return healthCheck;
	}

	public void setHealthCheck(HealthCheck healthCheck) {
		this.healthCheck = healthCheck;
	}

	public static class HealthCheck {

		/**
		 * Initial delay value for the HealthCheck scheduler.
		 */
		private int initialDelay = 0;

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

		public int getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(int initialDelay) {
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

}
