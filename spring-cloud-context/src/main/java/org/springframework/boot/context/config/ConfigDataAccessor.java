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

package org.springframework.boot.context.config;

import java.util.Arrays;
import java.util.function.Supplier;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

public class ConfigDataAccessor {

	private final ConfigDataEnvironment configDataEnvironment;

	/**
	 * Create a new {@link ConfigDataEnvironment} instance.
	 * @param environment the Spring {@link Environment}.
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 * @param additionalProfiles any additional profiles to activate
	 */
	public ConfigDataAccessor(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String[] additionalProfiles) {
		configDataEnvironment = new ConfigDataEnvironment(Supplier::get, environment,
				resourceLoader, Arrays.asList(additionalProfiles));
	}

	public void applyToEnvironment() {
		configDataEnvironment.processAndApply();
	}

}
