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

import org.springframework.context.ApplicationEvent;

/**
 * An event that a DiscoveryClient implementation can broadcast if it supports heartbeats
 * from the discovery server. Provides listeners with a basic indication of a state change
 * in the service catalog.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class HeartbeatEvent extends ApplicationEvent {

	private final Object state;

	/**
	 * Creates a new event with a source (for example, a discovery client) and a value.
	 * Neither parameter should be relied on to have specific content or format.
	 * @param source The source of the event.
	 * @param state The value indicating state of the catalog.
	 */
	public HeartbeatEvent(Object source, Object state) {
		super(source);
		this.state = state;
	}

	/**
	 * A value representing the state of the service catalog. The only requirement is that
	 * it changes when the catalog is updated; it can be as simple as a version counter or
	 * a hash. Implementations can provide information to help users visualize what is
	 * going on in the catalog, but users should not rely on the content (since the
	 * implementation of the underlying discovery might change).
	 * @return A value representing state of the service catalog.
	 */
	public Object getValue() {
		return this.state;
	}

}
