/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.util;

import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

public abstract class PropertyUtils {

	/**
	 * Property name for checking if bootstrap is enabled.
	 */
	public static final String BOOTSTRAP_ENABLED_PROPERTY = "spring.cloud.bootstrap.enabled";

	/**
	 * Property name for spring boot legacy processing.
	 */
	public static final String USE_LEGACY_PROCESSING_PROPERTY = "spring.config.use-legacy-processing";

	/**
	 * Property name for bootstrap marker class name.
	 */
	public static final String MARKER_CLASS = "org.springframework.cloud.bootstrap.marker.Marker";

	/**
	 * Boolean if bootstrap marker class exists.
	 */
	public static final boolean MARKER_CLASS_EXISTS = ClassUtils.isPresent(MARKER_CLASS, null);

	private PropertyUtils() {
		throw new UnsupportedOperationException("unable to instatiate utils class");
	}

	public static boolean bootstrapEnabled(Environment environment) {
		return environment.getProperty(BOOTSTRAP_ENABLED_PROPERTY, Boolean.class, false) || MARKER_CLASS_EXISTS;
	}

	public static boolean useLegacyProcessing(Environment environment) {
		return environment.getProperty(USE_LEGACY_PROCESSING_PROPERTY, Boolean.class, false);
	}

}
