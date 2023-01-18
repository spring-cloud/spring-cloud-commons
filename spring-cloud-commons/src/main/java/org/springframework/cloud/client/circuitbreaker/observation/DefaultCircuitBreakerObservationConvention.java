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

import java.util.Locale;

import io.micrometer.common.KeyValues;

/**
 * Default implementation of {@link CircuitBreakerObservationContext}.
 *
 * @author Marcin Grzejszczak
 * @since 4.0.0
 */
public class DefaultCircuitBreakerObservationConvention implements CircuitBreakerObservationConvention {

	/**
	 * Don't know why this needs to be public.
	 */
	public static final DefaultCircuitBreakerObservationConvention INSTANCE = new DefaultCircuitBreakerObservationConvention();

	@Override
	public KeyValues getLowCardinalityKeyValues(CircuitBreakerObservationContext context) {
		return KeyValues.of(CircuitBreakerObservationDocumentation.LowCardinalityTags.OBJECT_TYPE
				.withValue(context.getType().name().toLowerCase(Locale.ROOT)));
	}

	@Override
	public String getName() {
		return "spring.cloud.circuitbreaker";
	}

	@Override
	public String getContextualName(CircuitBreakerObservationContext context) {
		if (context.getType() == CircuitBreakerObservationContext.Type.SUPPLIER) {
			return "circuit-breaker";
		}
		return "circuit-breaker fallback";
	}

}
