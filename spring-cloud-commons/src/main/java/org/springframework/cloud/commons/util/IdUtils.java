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

package org.springframework.cloud.commons.util;

import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 */
public final class IdUtils {

	private static final String SEPARATOR = ":";

	// @checkstyle:off
	public static final String DEFAULT_SERVICE_ID_STRING = "${vcap.application.name:${spring.application.name:application}}:${vcap.application.instance_index:${spring.application.index:${local.server.port:${server.port:8080}}}}:${vcap.application.instance_id:${cachedrandom.${vcap.application.name:${spring.application.name:application}}.value}}";

	public static final String DEFAULT_SERVICE_ID_WITH_ACTIVE_PROFILES_STRING = "${vcap.application.name:${spring.application.name:application}:${spring.profiles.active}}:${vcap.application.instance_index:${spring.application.index:${local.server.port:${server.port:8080}}}}:${vcap.application.instance_id:${cachedrandom.${vcap.application.name:${spring.application.name:application}}.value}}";

	// @checkstyle:on

	private IdUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static String getDefaultInstanceId(PropertyResolver resolver) {
		return getDefaultInstanceId(resolver, true);
	}

	public static String getDefaultInstanceId(PropertyResolver resolver, boolean includeHostname) {
		String vcapInstanceId = resolver.getProperty("vcap.application.instance_id");
		if (StringUtils.hasText(vcapInstanceId)) {
			return vcapInstanceId;
		}

		String hostname = null;
		if (includeHostname) {
			hostname = resolver.getProperty("spring.cloud.client.hostname");
		}
		String appName = resolver.getProperty("spring.application.name");

		String namePart = combineParts(hostname, SEPARATOR, appName);

		String indexPart = resolver.getProperty("spring.application.instance_id", resolver.getProperty("server.port"));

		return combineParts(namePart, SEPARATOR, indexPart);
	}

	/**
	 * Gets the resolved service id.
	 * @param resolver A property resolved
	 * @return A unique id that can be used to uniquely identify a service
	 */
	public static String getResolvedServiceId(PropertyResolver resolver) {
		final String unresolvedServiceId;
		// addition of active profiles at the 2nd position of the service ID breaks
		// backwards-compatibility,
		// so we fall back to the old implementation in case no profiles are active
		if (StringUtils.hasText(resolver.getProperty("spring.profiles.active"))) {
			unresolvedServiceId = getUnresolvedServiceIdWithActiveProfiles();
		}
		else {
			unresolvedServiceId = getUnresolvedServiceId();
		}
		return resolver.resolvePlaceholders(unresolvedServiceId);
	}

	/**
	 * Gets the unresolved template for the service id <i>without active profiles.</i>
	 * @return The combination of properties to create a unique service id.
	 */
	public static String getUnresolvedServiceId() {
		return DEFAULT_SERVICE_ID_STRING;
	}

	/**
	 * Gets the unresolved template for the service id including active profiles.
	 * @return The combination of properties to create a unique service id.
	 */
	public static String getUnresolvedServiceIdWithActiveProfiles() {
		return DEFAULT_SERVICE_ID_WITH_ACTIVE_PROFILES_STRING;
	}

	public static String combineParts(String firstPart, String separator, String secondPart) {
		String combined = null;
		if (firstPart != null && secondPart != null) {
			combined = firstPart + separator + secondPart;
		}
		else if (firstPart != null) {
			combined = firstPart;
		}
		else if (secondPart != null) {
			combined = secondPart;
		}
		return combined;
	}

}
