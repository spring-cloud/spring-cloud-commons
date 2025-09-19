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

package org.springframework.cloud.client.discovery.health.reactive;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.health.contributor.CompositeReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.util.Assert;

/**
 * A composite health contributor specific to a reactive discovery client implementation.
 *
 * @author Tim Ysewyn
 */
public class ReactiveDiscoveryCompositeHealthContributor implements CompositeReactiveHealthContributor {

	private Map<String, ReactiveDiscoveryHealthIndicator> indicators;

	public ReactiveDiscoveryCompositeHealthContributor(Collection<ReactiveDiscoveryHealthIndicator> indicators) {
		Assert.notNull(indicators, "'indicators' must not be null");
		this.indicators = indicators.stream()
			.collect(Collectors.toMap(ReactiveDiscoveryHealthIndicator::getName, Function.identity()));
	}

	@Override
	public Stream<Entry> stream() {
		return indicators.entrySet()
			.stream()
			.map((entry) -> new Entry(entry.getKey(), asHealthIndicator(entry.getValue())));
	}

	@Override
	public ReactiveHealthContributor getContributor(String name) {
		return asHealthIndicator(indicators.get(name));
	}

	private ReactiveHealthIndicator asHealthIndicator(ReactiveDiscoveryHealthIndicator indicator) {
		return (indicator != null) ? indicator::health : null;
	}

}
