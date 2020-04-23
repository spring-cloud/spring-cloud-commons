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

package org.springframework.cloud.context.environment;

import java.util.Set;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.env.Environment;

/**
 * Event published to signal a change in the {@link Environment}.
 *
 * @author Dave Syer
 *
 */
@SuppressWarnings("serial")
public class EnvironmentChangeEvent extends ApplicationEvent {

	private Set<String> keys;

	public EnvironmentChangeEvent(Set<String> keys) {
		// Backwards compatible constructor with less utility (practically no use at all)
		this(keys, keys);
	}

	public EnvironmentChangeEvent(Object context, Set<String> keys) {
		super(context);
		this.keys = keys;
	}

	/**
	 * @return The keys.
	 */
	public Set<String> getKeys() {
		return this.keys;
	}

}
