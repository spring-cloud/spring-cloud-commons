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

package org.springframework.cloud.context.scope;

import java.util.Collection;

/**
 * A special-purpose cache interface specifically for the {@link GenericScope} to use to
 * manage cached bean instances. Implementations generally fall into two categories: those
 * that store values "globally" (i.e. one instance per key), and those that store
 * potentially multiple instances per key based on context (e.g. via a thread local). All
 * implementations should be thread safe.
 *
 * @author Dave Syer
 *
 */
public interface ScopeCache {

	/**
	 * Removes the object with this name from the cache.
	 * @param name The object name.
	 * @return The object removed, or null if there was none.
	 */
	Object remove(String name);

	/**
	 * Clears the cache and returns all objects in an unmodifiable collection.
	 * @return All objects stored in the cache.
	 */
	Collection<Object> clear();

	/**
	 * Gets the named object from the cache.
	 * @param name The name of the object.
	 * @return The object with that name, or null if there is none.
	 */
	Object get(String name);

	/**
	 * Put a value in the cache if the key is not already used. If one is already present
	 * with the name provided, it is not replaced, but is returned to the caller.
	 * @param name The key.
	 * @param value The new candidate value.
	 * @return The value that is in the cache at the end of the operation.
	 */
	Object put(String name, Object value);

}
