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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * Observed {@link Function}.
 *
 * @param <T> type returned by the fallback
 * @since 4.0.0
 */
class ObservedFunction<T> implements Function<Throwable, T> {

	private final Function<Throwable, T> delegate;

	private final Observation observation;

	ObservedFunction(CircuitBreakerObservationConvention customConvention, CircuitBreakerObservationContext context,
			String conextualName, ObservationRegistry observationRegistry, Function<Throwable, T> toRun) {
		this.delegate = toRun;
		this.observation = CircuitBreakerObservationDocumentation.CIRCUIT_BREAKER_SUPPLIER_OBSERVATION
				.observation(customConvention, DefaultCircuitBreakerObservationConvention.INSTANCE, () -> context,
						observationRegistry)
				.parentObservation(observationRegistry.getCurrentObservation());
		this.observation.contextualName(conextualName);
	}

	@Override
	public T apply(Throwable throwable) {
		return this.observation.observe(() -> this.delegate.apply(throwable));
	}

}
