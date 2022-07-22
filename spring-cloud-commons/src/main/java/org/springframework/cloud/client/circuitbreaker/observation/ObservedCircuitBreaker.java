/*
 * Copyright 2018-2021 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

/**
 * Observed Circuit Breaker.
 *
 * @author Marcin Grzejszczak
 * @since 4.0.0
 */
public class ObservedCircuitBreaker implements CircuitBreaker {

	private final CircuitBreaker delegate;

	private final ObservationRegistry observationRegistry;

	private CircuitBreakerObservationConvention customConvention;

	public ObservedCircuitBreaker(CircuitBreaker delegate, ObservationRegistry observationRegistry) {
		this.delegate = delegate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		return this.delegate.run(
				new ObservedSupplier<>(this.customConvention,
						new CircuitBreakerObservationContext(CircuitBreakerObservationContext.Type.SUPPLIER),
						"circuit-breaker", this.observationRegistry, toRun),
				new ObservedFunction<>(this.customConvention,
						new CircuitBreakerObservationContext(CircuitBreakerObservationContext.Type.FUNCTION),
						"circuit-breaker fallback", this.observationRegistry, fallback));
	}

	@Override
	public <T> T run(Supplier<T> toRun) {
		return this.delegate.run(new ObservedSupplier<>(this.customConvention,
				new CircuitBreakerObservationContext(CircuitBreakerObservationContext.Type.SUPPLIER), "circuit-breaker",
				this.observationRegistry, toRun));
	}

	public void setCustomConvention(CircuitBreakerObservationConvention customConvention) {
		this.customConvention = customConvention;
	}

}
