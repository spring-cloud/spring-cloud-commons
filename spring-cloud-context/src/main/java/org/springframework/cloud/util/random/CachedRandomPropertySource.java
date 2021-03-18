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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Ryan Baxter
 */
public class CachedRandomPropertySource extends PropertySource<PropertySource> {

	static final String NAME = "cachedrandom";

	private static final String PREFIX = NAME + ".";

	private static Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

	public CachedRandomPropertySource(PropertySource randomValuePropertySource) {
		super(NAME, randomValuePropertySource);

	}

	CachedRandomPropertySource(PropertySource randomValuePropertySource, Map<String, Map<String, Object>> cache) {
		super(NAME, randomValuePropertySource);
		this.cache = cache;
	}

	@Override
	public Object getProperty(String name) {
		if (!name.startsWith(PREFIX) || name.length() == PREFIX.length()) {
			return null;
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Generating random property for '" + name + "'");
			}
			// TO avoid any weirdness from the type or key including a "." we look for the
			// last "." and substring everything instead of splitting on the "."
			String keyAndType = name.substring(PREFIX.length());
			int lastIndexOfDot = keyAndType.lastIndexOf(".");
			if (lastIndexOfDot < 0) {
				return null;
			}
			String key = keyAndType.substring(0, lastIndexOfDot);
			String type = keyAndType.substring(lastIndexOfDot + 1);
			if (StringUtils.hasText(key) && StringUtils.hasText(type)) {
				return getRandom(type, key);
			}
			else {
				return null;
			}
		}
	}

	private Object getRandom(String type, String key) {
		Map<String, Object> randomValueCache = getCacheForKey(key);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking in random cache for key " + key + " with type " + type);
		}
		return randomValueCache.computeIfAbsent(type, (theType) -> {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"No random value found in cache for key: %s and type: %s, generating a new value", key, type));
			}
			return getSource().getProperty("random." + type);
		});
	}

	private Map<String, Object> getCacheForKey(String key) {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking in random cache for key: " + key);
		}
		return cache.computeIfAbsent(key, theKey -> {
			if (logger.isDebugEnabled()) {
				logger.debug("No cached value found for key: " + key);
			}
			return new ConcurrentHashMap<>();
		});
	}

	public static void clearCache() {
		if (cache != null) {
			cache.clear();
		}
	}

}
