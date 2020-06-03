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

package org.springframework.cloud.client.loadbalancer.reactive;

/**
 * Allows to propagate data related to load-balanced call completion status.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public interface CompletionContext {

	/**
	 * Returns the execution status of a request executed via SC LoadBalancer.
	 * @return a {@link Status}
	 */
	Status getStatus();

	/**
	 * If an exception has occurred, returns the associated {@link Throwable}.
	 * @return a {@link Throwable} object
	 */
	Throwable getThrowable();

	/**
	 * Returns the response of the request executed against the instance returned by SC
	 * LoadBalancer.
	 * @return
	 */
	Object getClientResponse();

	/**
	 * Request status state.
	 */
	enum Status {

		/** Request was handled successfully. */
		SUCCESS,
		/** Request reached the server but failed due to timeout or internal error. */
		FAILED,
		/** Request did not go off box and should not be counted for statistics. */
		DISCARD,

	}

}
