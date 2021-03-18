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

package org.springframework.cloud.util.random;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 */
@ExtendWith(MockitoExtension.class)
@DirtiesContext
public class CachedRandomPropertySourceTests {

	@Mock
	private PropertySource randomValuePropertySource;

	@BeforeEach
	public void setup() {
		when(randomValuePropertySource.getProperty(eq("random.long"))).thenReturn(1234L);
	}

	@Test
	public void getProperty() {
		HashMap<String, AtomicInteger> keyCount = new HashMap<>();
		HashMap<Map<String, Object>, String> typeToKeyLookup = new HashMap<>();
		HashMap<String, AtomicInteger> typeCount = new HashMap<>();
		Map<String, Map<String, Object>> cache = createCache(keyCount, typeToKeyLookup, typeCount);
		Map<String, Object> typeCache = createTypeCache(typeToKeyLookup, typeCount);
		typeCache.put("long", 5678L);
		cache.put("foo", typeCache);

		CachedRandomPropertySource cachedRandomPropertySource = new CachedRandomPropertySource(
				randomValuePropertySource, cache);
		then(cachedRandomPropertySource.getProperty("foo.app.long")).isNull();
		then(cachedRandomPropertySource.getProperty("cachedrandom.app")).isNull();

		then(cachedRandomPropertySource.getProperty("cachedrandom.app.long")).isEqualTo(1234L);
		then(cachedRandomPropertySource.getProperty("cachedrandom.foo.long")).isEqualTo(5678L);
		then(keyCount).containsOnlyKeys("app"); // verifies foo wasn't computed
		then(keyCount.get("app").get()).isEqualTo(1);
		then(typeCount).containsOnlyKeys("app.long"); // verifies foo.long wasn't computed
		then(typeCount.get("app.long").get()).isEqualTo(1);
	}

	private HashMap<String, Map<String, Object>> createCache(HashMap<String, AtomicInteger> keyCount,
			HashMap<Map<String, Object>, String> typeToKeyLookup, HashMap<String, AtomicInteger> typeCount) {
		return new HashMap<String, Map<String, Object>>() {
			@Override
			public Map<String, Object> computeIfAbsent(String key,
					Function<? super String, ? extends Map<String, Object>> mappingFunction) {
				boolean computed = false;
				if (!containsKey(key)) {
					keyCount.computeIfAbsent(key, s -> new AtomicInteger()).incrementAndGet();
					computed = true;
				}
				Map<String, Object> value = super.computeIfAbsent(key, mappingFunction);
				if (computed && value.isEmpty()) {
					// replace value with instrumented map
					value = createTypeCache(typeToKeyLookup, typeCount);
					typeToKeyLookup.put(value, key);
				}
				return value;
			}
		};
	}

	private HashMap<String, Object> createTypeCache(HashMap<Map<String, Object>, String> typeToKeyLookup,
			HashMap<String, AtomicInteger> typeCount) {
		return new HashMap<String, Object>() {
			@Override
			public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
				if (!containsKey(key)) {
					String parentKey = typeToKeyLookup.get(this);
					typeCount.computeIfAbsent(parentKey + "." + key, s -> new AtomicInteger()).incrementAndGet();
				}
				return super.computeIfAbsent(key, mappingFunction);
			}
		};
	}

}
