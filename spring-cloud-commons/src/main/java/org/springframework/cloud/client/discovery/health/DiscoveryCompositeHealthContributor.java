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

package org.springframework.cloud.client.discovery.health;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * Adapter that converts a collection of {@link DiscoveryHealthIndicator} beans into a
 * {@link CompositeHealthContributor}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class DiscoveryCompositeHealthContributor implements CompositeHealthContributor {

	private Map<String, DiscoveryHealthIndicator> indicators;

	public DiscoveryCompositeHealthContributor(Collection<DiscoveryHealthIndicator> indicators) {
		Assert.notNull(indicators, "'indicators' must not be null");
		this.indicators = indicators.stream()
			.collect(Collectors.toMap(DiscoveryHealthIndicator::getName, Function.identity()));
	}

	@Override
	public Stream<Entry> stream() {
		return indicators.entrySet()
			.stream()
			.map((entry) -> new Entry(entry.getKey(), asHealthIndicator(entry.getValue())));
	}

	@Override
	public HealthContributor getContributor(String name) {
		return asHealthIndicator(this.indicators.get(name));
	}

	private HealthIndicator asHealthIndicator(DiscoveryHealthIndicator indicator) {
		return (indicator != null) ? indicator::health : null;
	}

	public Map<String, DiscoveryHealthIndicator> getIndicators() {
		return Collections.unmodifiableMap(indicators);
	}

}
