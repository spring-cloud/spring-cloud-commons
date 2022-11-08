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

package org.springframework.cloud.context.scope.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.context.scope.ScopeCache;

/**
 * @author Dave Syer
 *
 */
public class ThreadLocalScopeCache implements ScopeCache {

	private ThreadLocal<Map<String, Object>> data = new ThreadLocal<Map<String, Object>>() {
		protected Map<String, Object> initialValue() {
			return new HashMap<String, Object>();
		}
	};

	public Object remove(String name) {
		return this.data.get().remove(name);
	}

	public Collection<Object> clear() {
		Map<String, Object> map = this.data.get();
		Collection<Object> values = new ArrayList<Object>(map.values());
		map.clear();
		return values;
	}

	public Object get(String name) {
		return this.data.get().get(name);
	}

	public Object put(String name, Object value) {
		Object result = this.data.get().putIfAbsent(name, value);
		if (result != null) {
			return result;
		}
		return value;
	}

}
