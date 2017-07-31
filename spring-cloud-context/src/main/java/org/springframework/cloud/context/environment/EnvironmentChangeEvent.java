/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Biju Kunjummen
 *
 */
@SuppressWarnings("serial")
public class EnvironmentChangeEvent extends ApplicationEvent {

	private final Set<String> keys;
	
	private final ChangeType changeType;

	public EnvironmentChangeEvent(Set<String> keys) {
		this(keys, ChangeType.ADD_UPDATE);
	}
	
	public EnvironmentChangeEvent(Set<String> keys, ChangeType changeType) {
		super(keys);
		this.keys = keys;
		this.changeType = changeType;
	}
	
	/**
	 * @return the keys
	 */
	public Set<String> getKeys() {
		return keys;
	}

	/**
	 * The type of change to the environment.
	 * Defaults to add/update
	 * 
	 * @return change type 
	 */
	public ChangeType getChangeType() {
		return this.changeType;
	}

	/**
	 * Represents the type of change for the set of keys.
	 */
	public enum ChangeType {
		ADD_UPDATE, DELETE
	}

}
