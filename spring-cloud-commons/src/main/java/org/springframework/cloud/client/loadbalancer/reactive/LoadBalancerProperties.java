/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A {@link ConfigurationProperties} bean for Spring Cloud LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 */
@ConfigurationProperties("spring.cloud.loadbalancer")
public class LoadBalancerProperties {

	/**
	 * A {@link String} representation of the <code>zone</code> used for filtering
	 * instances by zoned load-balancing implementations.
	 */
	private String zone;

	/**
	 * Properties for <code>HealthCheckServiceInstanceListSupplier</code>.
	 */
	private HealthCheck healthCheck = new HealthCheck();

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

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
		 * Period for rerunning the HealthCheck scheduler.
		 */
		private int period = 30;

		/**
		 * TimeUnit for rerunning the HealthCheck scheduler.
		 */
		private TimeUnit unit = TimeUnit.SECONDS;

		public int getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(int initialDelay) {
			this.initialDelay = initialDelay;
		}

		public int getPeriod() {
			return period;
		}

		public void setPeriod(int period) {
			this.period = period;
		}

		public TimeUnit getUnit() {
			return unit;
		}

		public void setUnit(TimeUnit unit) {
			this.unit = unit;
		}

	}

}
