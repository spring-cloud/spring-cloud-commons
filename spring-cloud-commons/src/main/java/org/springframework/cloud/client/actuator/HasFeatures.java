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

package org.springframework.cloud.client.actuator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Spencer Gibb
 */
public class HasFeatures {

	private final List<Class<?>> abstractFeatures = new ArrayList<>();

	private final List<NamedFeature> namedFeatures = new ArrayList<>();

	public HasFeatures(List<Class<?>> abstractFeatures, List<NamedFeature> namedFeatures) {
		this.abstractFeatures.addAll(abstractFeatures);
		this.namedFeatures.addAll(namedFeatures);
	}

	public static HasFeatures abstractFeatures(Class<?>... abstractFeatures) {
		return new HasFeatures(Arrays.asList(abstractFeatures), Collections.<NamedFeature>emptyList());
	}

	public static HasFeatures namedFeatures(NamedFeature... namedFeatures) {
		return new HasFeatures(Collections.<Class<?>>emptyList(), Arrays.asList(namedFeatures));
	}

	public static HasFeatures namedFeature(String name, Class<?> type) {
		return namedFeatures(new NamedFeature(name, type));
	}

	public static HasFeatures namedFeatures(String name1, Class<?> type1, String name2, Class<?> type2) {
		return namedFeatures(new NamedFeature(name1, type1), new NamedFeature(name2, type2));
	}

	public List<Class<?>> getAbstractFeatures() {
		return this.abstractFeatures;
	}

	public List<NamedFeature> getNamedFeatures() {
		return this.namedFeatures;
	}

}
