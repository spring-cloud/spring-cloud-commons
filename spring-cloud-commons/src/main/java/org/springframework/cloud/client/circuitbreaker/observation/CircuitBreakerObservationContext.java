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

import io.micrometer.observation.Observation;

/**
 * Circuit Breaker {@link Observation.Context}.
 *
 * @author Marcin Grzejszczak
 * @since 4.0.0
 */
public class CircuitBreakerObservationContext extends Observation.Context {

	private final Type type;

	/**
	 * Creates a new instance of {@link CircuitBreakerObservationDocumentation}.
	 * @param type type of wrapped object
	 */
	public CircuitBreakerObservationContext(Type type) {
		this.type = type;
	}

	/**
	 * Gets the wrapped object type.
	 * @return type of wrapped object
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Describes the type of wrapped object.
	 */
	public enum Type {

		/**
		 * Fallback function.
		 */
		FUNCTION,

		/**
		 * Operation to run.
		 */
		SUPPLIER

	}

}
