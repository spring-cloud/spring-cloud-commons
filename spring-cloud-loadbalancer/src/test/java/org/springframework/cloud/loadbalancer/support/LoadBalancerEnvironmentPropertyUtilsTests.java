/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.cloud.loadbalancer.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadBalancerEnvironmentPropertyUtils}.
 *
 * @author Olga Maciaszek-Sharma
 */
class LoadBalancerEnvironmentPropertyUtilsTests {

	MockEnvironment environment = new MockEnvironment();

	@BeforeEach
	void setUp() {
		environment.setProperty("loadbalancer.client.name", "x");
	}

	@Test
	void shouldReturnTrueWhenTrueForClient() {
		environment.setProperty("spring.cloud.loadbalancer.clients.x.test", "true");
		environment.setProperty("spring.cloud.loadbalancer.test", "false");

		assertThat(LoadBalancerEnvironmentPropertyUtils.trueForClientOrDefault(environment, "test")).isTrue();
		assertThat(LoadBalancerEnvironmentPropertyUtils.trueOrMissingForClientOrDefault(environment, "test")).isTrue();
	}

	@Test
	void shouldReturnTrueWhenTrueForDefault() {
		environment.setProperty("spring.cloud.loadbalancer.test", "true");

		assertThat(LoadBalancerEnvironmentPropertyUtils.trueForClientOrDefault(environment, "test")).isTrue();
		assertThat(LoadBalancerEnvironmentPropertyUtils.trueOrMissingForClientOrDefault(environment, "test")).isTrue();
	}

	@Test
	void shouldReturnTrueWhenEqualForClient() {
		environment.setProperty("spring.cloud.loadbalancer.clients.x.test", "true");
		environment.setProperty("spring.cloud.loadbalancer.test", "false");

		assertThat(LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(environment, "test", "true"))
				.isTrue();
		assertThat(LoadBalancerEnvironmentPropertyUtils.equalToOrMissingForClientOrDefault(environment, "test", "true"))
				.isTrue();
	}

	@Test
	void shouldReturnTrueWhenEqualForDefault() {
		environment.setProperty("spring.cloud.loadbalancer.test", "true");

		assertThat(LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(environment, "test", "true"))
				.isTrue();
		assertThat(LoadBalancerEnvironmentPropertyUtils.equalToOrMissingForClientOrDefault(environment, "test", "true"))
				.isTrue();
	}

	@Test
	void shouldReturnTrueWhenMissingForClientAndDefault() {
		assertThat(LoadBalancerEnvironmentPropertyUtils.trueOrMissingForClientOrDefault(environment, "test")).isTrue();
		assertThat(LoadBalancerEnvironmentPropertyUtils.equalToOrMissingForClientOrDefault(environment, "test", "true"))
				.isTrue();
	}

}
