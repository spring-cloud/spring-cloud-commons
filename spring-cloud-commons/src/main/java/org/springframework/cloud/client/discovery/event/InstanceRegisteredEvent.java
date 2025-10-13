/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * Event to be published after the local service instance registers itself with a
 * discovery service.
 *
 * @param <T> - type of configuration
 * @author Spencer Gibb
 * @author Yanming Zhou
 */
@SuppressWarnings("serial")
public class InstanceRegisteredEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {

	private T config;

	/**
	 * Creates a new {@link InstanceRegisteredEvent} instance.
	 * @param source The component that published the event (never {@code null}).
	 * @param config The configuration of the instance.
	 */
	public InstanceRegisteredEvent(Object source, T config) {
		super(source);
		this.config = config;
	}

	public T getConfig() {
		return this.config;
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(this.config));
	}

}
