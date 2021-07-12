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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DiscoveryCompositeHealthContributor}.
 *
 * @author Phillip Webb
 */
public class DiscoveryCompositeHealthContributorTests {

	@Test
	public void createWhenIndicatorsAreNullThrowsException() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() -> new DiscoveryCompositeHealthContributor(null))
				.withMessage("'indicators' must not be null");
	}

	@Test
	public void getContributorReturnsContributor() throws Exception {
		TestDiscoveryHealthIndicator indicator = new TestDiscoveryHealthIndicator("test", Health.up().build());
		DiscoveryCompositeHealthContributor composite = new DiscoveryCompositeHealthContributor(
				Arrays.asList(indicator));
		HealthIndicator adapted = (HealthIndicator) composite.getContributor("test");
		assertThat(adapted).isNotNull();
		assertThat(adapted.health()).isSameAs(indicator.health());
	}

	@Test
	public void getContributorWhenMissingReturnsNull() throws Exception {
		TestDiscoveryHealthIndicator indicator = new TestDiscoveryHealthIndicator("test", Health.up().build());
		DiscoveryCompositeHealthContributor composite = new DiscoveryCompositeHealthContributor(
				Arrays.asList(indicator));
		assertThat((HealthIndicator) composite.getContributor("missing")).isNull();
	}

	@Test
	public void iteratorIteratesNamedContributors() throws Exception {
		TestDiscoveryHealthIndicator indicator1 = new TestDiscoveryHealthIndicator("test1", Health.up().build());
		TestDiscoveryHealthIndicator indicator2 = new TestDiscoveryHealthIndicator("test2", Health.down().build());
		DiscoveryCompositeHealthContributor composite = new DiscoveryCompositeHealthContributor(
				Arrays.asList(indicator1, indicator2));
		List<NamedContributor<HealthContributor>> contributors = new ArrayList<>();
		for (NamedContributor<HealthContributor> contributor : composite) {
			contributors.add(contributor);
		}
		assertThat(contributors).hasSize(2);
		assertThat(contributors).extracting("name").containsExactlyInAnyOrder("test1", "test2");
		// TODO: HealthContributor no longer has a health method
		// assertThat(contributors).extracting("contributor").extracting("health")
		// .containsExactlyInAnyOrder(indicator1.health(), indicator2.health());
	}

	private static class TestDiscoveryHealthIndicator implements DiscoveryHealthIndicator {

		private final String name;

		private final Health health;

		TestDiscoveryHealthIndicator(String name, Health health) {
			super();
			this.name = name;
			this.health = health;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Health health() {
			return this.health;
		}

	}

}
