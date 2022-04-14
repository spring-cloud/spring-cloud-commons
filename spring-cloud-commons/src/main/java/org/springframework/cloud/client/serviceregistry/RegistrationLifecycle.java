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

package org.springframework.cloud.client.serviceregistry;

import org.springframework.core.Ordered;

/**
 * Service registration life cycle. This life cycle is only related to
 * {@link Registration}.
 *
 * @author Zen Huifer
 */
public interface RegistrationLifecycle<R extends Registration> extends Ordered {

	/**
	 * default order.
	 */
	int DEFAULT_ORDER = 0;

	/**
	 * A method executed before registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessBeforeStartRegister(R registration);

	/**
	 * A method executed after registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessAfterStartRegister(R registration);

	/**
	 * A method executed before de-registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessBeforeStopRegister(R registration);

	/**
	 * A method executed after de-registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessAfterStopRegister(R registration);

	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
