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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
@DirtiesContext
public class CachedRandomPropertySourceTests {

	@Mock
	RandomValuePropertySource randomValuePropertySource;

	@Before
	public void setup() {

		when(randomValuePropertySource.getProperty(eq("random.long")))
				.thenReturn(new Long(1234));
	}

	@Test
	public void getProperty() {
		Map<String, Map<String, Object>> cache = new HashMap<>();
		Map<String, Object> typeCache = new HashMap<>();
		typeCache.put("long", new Long(5678));
		Map<String, Object> spyedTypeCache = spy(typeCache);
		cache.put("foo", spyedTypeCache);
		Map<String, Map<String, Object>> spyedCache = spy(cache);

		CachedRandomPropertySource cachedRandomPropertySource = new CachedRandomPropertySource(
				randomValuePropertySource, spyedCache);
		then(cachedRandomPropertySource.getProperty("foo.app.long")).isNull();
		then(cachedRandomPropertySource.getProperty("cachedrandom.app")).isNull();

		then(cachedRandomPropertySource.getProperty("cachedrandom.app.long"))
				.isEqualTo(new Long(1234));
		then(cachedRandomPropertySource.getProperty("cachedrandom.foo.long"))
				.isEqualTo(new Long(5678));
		verify(spyedCache, times(1)).put(eq("app"), isA(Map.class));
		verify(spyedTypeCache, times(1)).get(eq("long"));
	}

}
