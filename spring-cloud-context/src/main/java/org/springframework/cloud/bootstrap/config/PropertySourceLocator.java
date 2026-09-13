/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.bootstrap.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;

/**
 * Strategy for locating (possibly remote) property sources for the Environment.
 * Implementations should not fail unless they intend to prevent the application from
 * starting.
 *
 * @author Dave Syer
 * @author Yanming Zhou
 *
 */
public interface PropertySourceLocator {

	/**
	 * @param environment The current Environment.
	 * @return A PropertySource, or null if there is none.
	 * @throws IllegalStateException if there is a fail-fast condition.
	 */
	PropertySource<?> locate(Environment environment);

	default Collection<PropertySource<?>> locateCollection(Environment environment) {
		return locateCollection(this, environment);
	}

	static Collection<PropertySource<?>> locateCollection(PropertySourceLocator locator, Environment environment) {
		PropertySource<?> propertySource = locator.locate(environment);
		if (propertySource == null) {
			return Collections.emptyList();
		}
		if (propertySource instanceof CompositePropertySource) {
			Collection<PropertySource<?>> sources = ((CompositePropertySource) propertySource).getPropertySources();
			List<PropertySource<?>> filteredSources = new ArrayList<>();
			for (PropertySource<?> p : sources) {
				if (p != null && shouldActivatePropertySource(p, environment)) {
					filteredSources.add(p);
				}
			}
			return filteredSources;
		}
		else {
			return shouldActivatePropertySource(propertySource, environment) ? List.of(propertySource)
					: Collections.emptyList();
		}
	}

	private static boolean shouldActivatePropertySource(PropertySource<?> ps, Environment environment) {
		String onProfile = (String) ps.getProperty("spring.config.activate.on-profile");
		if ((onProfile != null) && !environment.acceptsProfiles(Profiles.of(onProfile))) {
			return false;
		}
		String onCloudPlatform = (String) ps.getProperty("spring.config.activate.on-cloud-platform");
		if (onCloudPlatform != null) {
			CloudPlatform cloudPlatform = CloudPlatform.getActive(environment);
			return cloudPlatform != null && cloudPlatform.name().equalsIgnoreCase(onCloudPlatform);
		}
		return true;
	}

}
