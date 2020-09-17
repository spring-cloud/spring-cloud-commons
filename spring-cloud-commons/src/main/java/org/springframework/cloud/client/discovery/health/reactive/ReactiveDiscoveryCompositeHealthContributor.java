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

package org.springframework.cloud.client.discovery.health.reactive;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
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
	public ReactiveHealthContributor getContributor(String name) {
		return asHealthIndicator(indicators.get(name));
	}

	@Override
	public Iterator<NamedContributor<ReactiveHealthContributor>> iterator() {
		return indicators.values().stream().map(this::asNamedContributor).iterator();
	}

	private NamedContributor<ReactiveHealthContributor> asNamedContributor(ReactiveDiscoveryHealthIndicator indicator) {
		return new NamedContributor<ReactiveHealthContributor>() {

			@Override
			public String getName() {
				return indicator.getName();
			}

			@Override
			public ReactiveHealthContributor getContributor() {
				return asHealthIndicator(indicator);
			}

		};
	}

	private ReactiveHealthIndicator asHealthIndicator(ReactiveDiscoveryHealthIndicator indicator) {
		return (indicator != null) ? indicator::health : null;
	}

}
