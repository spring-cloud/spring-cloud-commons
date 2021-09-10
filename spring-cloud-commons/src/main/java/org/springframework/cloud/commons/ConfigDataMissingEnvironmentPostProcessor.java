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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

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
		List<Object> property = getConfigImports(environment);
		if (property == null || property.isEmpty()) {
			throw new ImportException("No spring.config.import set", false);
		}
		if (!property.stream().anyMatch(impt -> ((String) impt).contains(getPrefix()))) {
			throw new ImportException("spring.config.import missing " + getPrefix(), true);
		}
	}

	private List<Object> getConfigImports(ConfigurableEnvironment environment) {
		MutablePropertySources propertySources = environment.getPropertySources();
		List<Object> property = propertySources.stream().filter(this::propertySourceWithConfigImport)
				.flatMap(propertySource -> {
					List<Object> configImports = new ArrayList<>();
					if (propertySource.getProperty(CONFIG_IMPORT_PROPERTY) != null) {
						configImports.add(propertySource.getProperty(CONFIG_IMPORT_PROPERTY));
					}
					else {
						configImports.addAll(Arrays.asList(getConfigImportArray(propertySource)));
					}
					return configImports.stream();
				}).collect(Collectors.toList());
		return property;
	}

	private boolean propertySourceWithConfigImport(PropertySource propertySource) {
		if (CompositePropertySource.class.isInstance(propertySource)) {
			return ((CompositePropertySource) propertySource).getPropertySources().stream()
					.anyMatch(this::propertySourceWithConfigImport);
		}
		return propertySource.containsProperty(CONFIG_IMPORT_PROPERTY)
				|| getConfigImportArray(propertySource).length > 0;
	}

	private String[] getConfigImportArray(PropertySource propertySource) {
		Binder binder = new Binder(ConfigurationPropertySource.from(propertySource));
		return binder.bind(CONFIG_IMPORT_PROPERTY, CONFIG_DATA_LOCATION_ARRAY).orElse(new String[0]);
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
