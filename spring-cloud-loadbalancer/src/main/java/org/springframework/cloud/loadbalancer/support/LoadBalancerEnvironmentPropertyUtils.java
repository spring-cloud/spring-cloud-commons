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

package org.springframework.cloud.loadbalancer.support;

import org.springframework.core.env.Environment;

/**
 * @author Olga Maciaszek-Sharma
 */
public final class LoadBalancerEnvironmentPropertyUtils {

	private LoadBalancerEnvironmentPropertyUtils() {
		throw new IllegalStateException("Should not instantiate a utility class");
	}

	public static boolean trueForClientOrDefault(Environment environment, String propertySuffix) {
		return equalToForClientOrDefault(environment, propertySuffix, Boolean.TRUE.toString());
	}

	public static boolean equalToForClientOrDefault(Environment environment, String propertySuffix,
			String expectedPropertyValue) {
		String defaultValue = getDefaultPropertyValue(environment, propertySuffix);
		String clientValue = getClientPropertyValue(environment, propertySuffix);
		if (clientValue != null && clientValue.equalsIgnoreCase(expectedPropertyValue)) {
			return true;
		}
		return clientValue == null && defaultValue != null && defaultValue.equalsIgnoreCase(expectedPropertyValue);
	}

	public static boolean equalToOrMissingForClientOrDefault(Environment environment, String propertySuffix,
			String expectedPropertyValue) {
		String defaultValue = getDefaultPropertyValue(environment, propertySuffix);
		String clientValue = getClientPropertyValue(environment, propertySuffix);
		if (clientValue != null && clientValue.equalsIgnoreCase(expectedPropertyValue)) {
			return true;
		}
		if (clientValue == null && defaultValue != null && defaultValue.equalsIgnoreCase(expectedPropertyValue)) {
			return true;
		}
		return clientValue == null && defaultValue == null;
	}

	public static boolean trueOrMissingForClientOrDefault(Environment environment, String propertySuffix) {
		return equalToOrMissingForClientOrDefault(environment, propertySuffix, Boolean.TRUE.toString());
	}

	private static String getClientPropertyValue(Environment environment, String propertySuffix) {
		return environment.getProperty("spring.cloud.loadbalancer.clients."
				+ LoadBalancerClientFactory.getName(environment) + "." + propertySuffix);
	}

	private static String getDefaultPropertyValue(Environment environment, String propertySuffix) {
		return environment.getProperty("spring.cloud.loadbalancer." + propertySuffix);
	}

}
