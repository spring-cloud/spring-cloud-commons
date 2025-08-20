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

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
class ReactiveDiscoveryCompositeHealthContributorTests {

	@Test
	void shouldReturnEmptyIterator() {
		ReactiveDiscoveryCompositeHealthContributor healthContributor = new ReactiveDiscoveryCompositeHealthContributor(
				emptyList());
		assertThat(healthContributor.iterator().hasNext()).isFalse();
	}

	@Test
	void shouldReturnNullForUnknownContributor() {
		ReactiveDiscoveryCompositeHealthContributor healthContributor = new ReactiveDiscoveryCompositeHealthContributor(
				emptyList());
		assertThat(healthContributor.getContributor("unknown")).isNull();
	}

	@Test
	void shouldReturnKnownContributor() {
		ReactiveDiscoveryHealthIndicator indicator = mock(ReactiveDiscoveryHealthIndicator.class);
		Health health = Health.up().build();
		when(indicator.getName()).thenReturn("known");
		when(indicator.health()).thenReturn(Mono.just(health));

		ReactiveDiscoveryCompositeHealthContributor healthContributor = new ReactiveDiscoveryCompositeHealthContributor(
				singletonList(indicator));

		assertThat(healthContributor.getContributor("known")).isNotNull();
		Iterator<ReactiveHealthContributors.Entry> iterator = healthContributor.iterator();
		assertThat(iterator.hasNext()).isTrue();
		ReactiveHealthContributors.Entry entry = iterator.next();
		assertThat(entry).isNotNull();
		assertThat(entry.name()).isEqualTo("known");
		assertThat(entry.contributor()).isNotNull();
		assertThat(entry.contributor()).isInstanceOf(ReactiveHealthIndicator.class);
		ReactiveHealthIndicator healthIndicator = (ReactiveHealthIndicator) entry.contributor();
		StepVerifier.create(healthIndicator.health(true)).expectNext(health).expectComplete().verify();
	}

}
