/*
 * Copyright 2012-2019 the original author or authors.
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

	private IdUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static String getDefaultInstanceId(PropertyResolver resolver) {
		return getDefaultInstanceId(resolver, true);
	}

	public static String getDefaultInstanceId(PropertyResolver resolver,
			boolean includeHostname) {
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

		String indexPart = resolver.getProperty("spring.application.instance_id",
				resolver.getProperty("server.port"));

		return combineParts(namePart, SEPARATOR, indexPart);
	}

	public static String combineParts(String firstPart, String separator,
			String secondPart) {
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
