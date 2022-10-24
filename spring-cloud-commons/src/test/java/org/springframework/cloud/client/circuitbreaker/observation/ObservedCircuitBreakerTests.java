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

package org.springframework.cloud.client.circuitbreaker.observation;

import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

import static org.assertj.core.api.BDDAssertions.then;

class ObservedCircuitBreakerTests {

	TestObservationRegistry registry = TestObservationRegistry.create();

	@Test
	void should_wrap_circuit_breaker_in_observation() {
		CircuitBreaker delegate = new CircuitBreaker() {
			@Override
			public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
				return toRun.get();
			}
		};
		ObservedCircuitBreaker circuitBreaker = new ObservedCircuitBreaker(delegate, registry);

		String result = circuitBreaker.run(() -> "hello");

		then(result).isEqualTo("hello");
		TestObservationRegistryAssert.assertThat(registry).hasSingleObservationThat()
				.hasNameEqualTo("spring.cloud.circuitbreaker")
				.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "supplier")
				.hasContextualNameEqualTo("circuit-breaker");
	}

	@Test
	void should_wrap_circuit_breaker_in_observation_with_custom_convention() {
		CircuitBreaker delegate = new CircuitBreaker() {
			@Override
			public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
				return toRun.get();
			}
		};
		ObservedCircuitBreaker circuitBreaker = new ObservedCircuitBreaker(delegate, registry);
		circuitBreaker.setCustomConvention(new CircuitBreakerObservationConvention() {

			@Override
			public KeyValues getLowCardinalityKeyValues(CircuitBreakerObservationContext context) {
				return KeyValues.of("bar", "baz");
			}

			@Override
			public String getName() {
				return "foo";
			}
		});

		String result = circuitBreaker.run(() -> "hello");

		then(result).isEqualTo("hello");
		TestObservationRegistryAssert.assertThat(registry).hasSingleObservationThat().hasNameEqualTo("foo")
				.hasLowCardinalityKeyValue("bar", "baz").hasContextualNameEqualTo("circuit-breaker");
	}

	@Test
	void should_wrap_circuit_breaker_with_fallback_in_observation() {
		TestObservationRegistry registry = TestObservationRegistry.create();
		CircuitBreaker delegate = new CircuitBreaker() {
			@Override
			public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
				try {
					return toRun.get();
				}
				catch (Throwable t) {
					return fallback.apply(t);
				}
			}
		};
		ObservedCircuitBreaker circuitBreaker = new ObservedCircuitBreaker(delegate, registry);

		Observation parent = Observation.createNotStarted("parent", registry);
		String result = parent.observe(() -> circuitBreaker.run(() -> {
			throw new IllegalStateException("BOOM!");
		}, throwable -> "goodbye"));

		then(result).isEqualTo("goodbye");

		TestObservationRegistryAssert.then(registry).hasNumberOfObservationsEqualTo(3)
				.hasHandledContextsThatSatisfy(contexts -> {
					ObservationContextAssert.then(contexts.get(0)).hasNameEqualTo("parent");
					ObservationContextAssert.then(contexts.get(1)).hasNameEqualTo("spring.cloud.circuitbreaker")
							.hasContextualNameEqualTo("circuit-breaker")
							.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "supplier")
							.hasParentObservationEqualTo(parent);
					ObservationContextAssert.then(contexts.get(2)).hasNameEqualTo("spring.cloud.circuitbreaker")
							.hasContextualNameEqualTo("circuit-breaker fallback")
							.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "function")
							.hasParentObservationEqualTo(parent);
				});
	}

}
