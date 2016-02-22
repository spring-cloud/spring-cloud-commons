/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.discovery.health;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Gathers all DiscoveryHealthIndicator's from a DiscoveryClient implementation
 * and aggregates the statuses.
 * @author Spencer Gibb
 */
//TODO: do we need this? Can they just be independent HealthIndicators?
public class DiscoveryCompositeHealthIndicator extends CompositeHealthIndicator {

	private final ArrayList<Holder> healthIndicators = new ArrayList<>();

	@Autowired
	public DiscoveryCompositeHealthIndicator(HealthAggregator healthAggregator,
			List<DiscoveryHealthIndicator> indicators) {
		super(healthAggregator);
		for (DiscoveryHealthIndicator indicator : indicators) {
			Holder holder = new Holder(indicator);
			addHealthIndicator(indicator.getName(), holder);
			healthIndicators.add(holder);
		}
	}

	public ArrayList<Holder> getHealthIndicators() {
		return healthIndicators;
	}

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
			return delegate;
		}
	}

}
