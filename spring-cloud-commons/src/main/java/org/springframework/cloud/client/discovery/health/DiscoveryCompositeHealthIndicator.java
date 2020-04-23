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

package org.springframework.cloud.client.discovery.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Gathers all instances of DiscoveryHealthIndicator from a DiscoveryClient implementation
 * and aggregates the statuses.
 *
 * @author Spencer Gibb
 * @deprecated since 2.2.0 in favor of {@link DiscoveryCompositeHealthContributor}
 */
// TODO: do we need this? Can they just be independent HealthIndicators?
@Deprecated
public class DiscoveryCompositeHealthIndicator extends CompositeHealthIndicator {

	private final ArrayList<Holder> healthIndicators = new ArrayList<>();

	@Autowired
	public DiscoveryCompositeHealthIndicator(HealthAggregator healthAggregator,
			List<DiscoveryHealthIndicator> indicators) {
		super(healthAggregator, createMap(indicators));
		getRegistry().getAll().values().stream()
				.filter(healthIndicator -> healthIndicator instanceof Holder)
				.forEach(healthIndicator -> this.healthIndicators
						.add((Holder) healthIndicator));
	}

	public ArrayList<Holder> getHealthIndicators() {
		return this.healthIndicators;
	}

	public static Map<String, HealthIndicator> createMap(
			List<DiscoveryHealthIndicator> indicators) {
		Map<String, HealthIndicator> map = new HashMap<>();
		for (DiscoveryHealthIndicator indicator : indicators) {
			Holder holder = new Holder(indicator);
			map.put(indicator.getName(), holder);
		}
		return map;
	}

	/**
	 * Holder for the Health Indicator.
	 */
	public static class Holder implements HealthIndicator {

		private DiscoveryHealthIndicator delegate;

		public Holder(DiscoveryHealthIndicator delegate) {
			this.delegate = delegate;
		}

		@Override
		public Health health() {
			return this.delegate.health();
		}

		public DiscoveryHealthIndicator getDelegate() {
			return this.delegate;
		}

	}

}
