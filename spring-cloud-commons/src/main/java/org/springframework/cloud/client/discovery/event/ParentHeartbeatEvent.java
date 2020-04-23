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
 * Heartbeat event that a parent ApplicationContext can send to a child context. Useful,
 * for example, when a config server is located via a DiscoveryClient, in which case the
 * {@link HeartbeatEvent} that triggers this event is fired in the parent (bootstrap)
 * context.
 *
 * @author Spencer Gibb
 */
@SuppressWarnings("serial")
// WARNING: do not extend HearbeatEvent because of a parent context forwarding
// Heartbeat events to a child. Avoids a stack overflow.
public class ParentHeartbeatEvent extends ApplicationEvent {

	private final Object value;

	public ParentHeartbeatEvent(Object source, Object value) {
		super(source);
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}

}
