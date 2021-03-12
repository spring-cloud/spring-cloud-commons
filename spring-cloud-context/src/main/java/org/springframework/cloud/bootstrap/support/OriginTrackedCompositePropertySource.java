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

package org.springframework.cloud.bootstrap.support;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

public class OriginTrackedCompositePropertySource extends CompositePropertySource implements OriginLookup<String> {

	/**
	 * Create a new {@code CompositePropertySource}.
	 * @param name the name of the property source
	 */
	public OriginTrackedCompositePropertySource(String name) {
		super(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Origin getOrigin(String name) {
		for (PropertySource<?> propertySource : getPropertySources()) {
			if (propertySource instanceof OriginLookup) {
				OriginLookup lookup = (OriginLookup) propertySource;
				Origin origin = lookup.getOrigin(name);
				if (origin != null) {
					return origin;
				}
			}
		}
		return null;
	}

}
