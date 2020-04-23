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

package org.springframework.cloud.client.discovery.event;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationEvent;

/**
 * An event to fire before a service is registered.
 *
 * @author Ryan Baxter
 */
public class InstancePreRegisteredEvent extends ApplicationEvent {

	private Registration registration;

	/**
	 * Create a new pre registration event.
	 * @param source the object on which the event initially occurred (never {@code null})
	 * @param registration the registration meta data
	 */
	public InstancePreRegisteredEvent(Object source, Registration registration) {
		super(source);
		this.registration = registration;
	}

	/**
	 * Get the registration data.
	 * @return the registration data
	 */
	public Registration getRegistration() {
		return this.registration;
	}

}
