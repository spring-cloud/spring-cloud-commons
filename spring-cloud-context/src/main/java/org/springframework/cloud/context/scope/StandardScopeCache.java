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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple cache implementation backed by a concurrent map.
 *
 * @author Dave Syer
 *
 */
public class StandardScopeCache implements ScopeCache {

	private final ConcurrentMap<String, Object> cache = new ConcurrentHashMap<String, Object>();

	@Override
	public Object remove(String name) {
		return this.cache.remove(name);
	}

	@Override
	public Collection<Object> clear() {
		Collection<Object> values = new ArrayList<Object>(this.cache.values());
		this.cache.clear();
		return values;
	}

	@Override
	public Object get(String name) {
		return this.cache.get(name);
	}

	@Override
	public Object put(String name, Object value) {
		Object result = this.cache.putIfAbsent(name, value);
		if (result != null) {
			return result;
		}
		return value;
	}

}
