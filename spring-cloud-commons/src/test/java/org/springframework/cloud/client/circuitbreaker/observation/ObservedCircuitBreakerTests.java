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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.assertj.core.api.BDDAssertions;
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
		ObservationRegistry registry = ObservationRegistry.create();
		MyHandler myHandler = new MyHandler();
		registry.observationConfig().observationHandler(myHandler);
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

		String result = circuitBreaker.run(() -> {
			throw new IllegalStateException("BOOM!");
		}, throwable -> "goodbye");

		then(result).isEqualTo("goodbye");
		List<Observation.Context> contexts = myHandler.contexts;

		// TODO: Convert to usage of test registry assert with the next micrometer release
		BDDAssertions.then(contexts).hasSize(2);
		BDDAssertions.then(contexts.get(0))
				.satisfies(context -> ObservationContextAssert.then(context)
						.hasNameEqualTo("spring.cloud.circuitbreaker").hasContextualNameEqualTo("circuit-breaker")
						.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "supplier"));
		BDDAssertions.then(contexts.get(1)).satisfies(context -> ObservationContextAssert.then(context)
				.hasNameEqualTo("spring.cloud.circuitbreaker").hasContextualNameEqualTo("circuit-breaker fallback")
				.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "function"));
	}

	// TODO: Convert to usage of test registry assert with the next micrometer release
	static class MyHandler implements ObservationHandler<Observation.Context> {

		List<Observation.Context> contexts = new ArrayList<>();

		@Override
		public void onStop(Observation.Context context) {
			this.contexts.add(context);
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return true;
		}

	}

}
