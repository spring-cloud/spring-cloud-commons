/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.commons;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Ryan Baxter
 */
public abstract class ConfigDataMissingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * Spring config import property name.
	 */
	public static final String CONFIG_IMPORT_PROPERTY = "spring.config.import";

	private static final Bindable<String[]> CONFIG_DATA_LOCATION_ARRAY = Bindable.of(String[].class);

	/**
	 * Order of post processor, set to run after
	 * {@link ConfigDataEnvironmentPostProcessor}.
	 */
	public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1000;

	@Override
	public int getOrder() {
		return ORDER;
	}

	protected abstract boolean shouldProcessEnvironment(Environment environment);

	protected abstract String getPrefix();

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!shouldProcessEnvironment(environment)) {
			return;
		}
		List<String> property = getConfigImports(environment);
		if (property == null || property.isEmpty()) {
			throw new ImportException("No spring.config.import set", false);
		}
		if (!property.stream().anyMatch(impt -> impt.contains(getPrefix()))) {
			throw new ImportException("spring.config.import missing " + getPrefix(), true);
		}
	}

	private List<String> getConfigImports(ConfigurableEnvironment environment) {
		List<String> property = environment.getProperty(CONFIG_IMPORT_PROPERTY, List.class);
		if (property == null || property.isEmpty()) {
			Binder binder = Binder.get(environment);
			property = Arrays
					.asList(binder.bind(CONFIG_IMPORT_PROPERTY, CONFIG_DATA_LOCATION_ARRAY).orElse(new String[0]));
		}
		return property;
	}

	public static class ImportException extends RuntimeException {

		/**
		 * Indicates if prefix is missing.
		 */
		public final boolean missingPrefix;

		ImportException(String message, boolean missingPrefix) {
			super(message);
			this.missingPrefix = missingPrefix;
		}

	}

}
