/*
 * Copyright 2013-2021 the original author or authors.
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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

enum CircuitBreakerObservationDocumentation implements ObservationDocumentation {

	/**
	 * Observation created when we wrap a Supplier passed to the CircuitBreaker.
	 */
	CIRCUIT_BREAKER_SUPPLIER_OBSERVATION {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultCircuitBreakerObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.cloud.circuitbreaker";
		}

	},

	/**
	 * Observation created when we wrap a Function passed to the CircuitBreaker as
	 * fallback.
	 */
	CIRCUIT_BREAKER_FUNCTION_OBSERVATION {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultCircuitBreakerObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.cloud.circuitbreaker";
		}

	};

	public enum LowCardinalityTags implements KeyName {

		/**
		 * Defines the type of wrapped lambda.
		 */
		OBJECT_TYPE {
			@Override
			public String asString() {
				return "spring.cloud.circuitbreaker.type";
			}
		}

	}

}
