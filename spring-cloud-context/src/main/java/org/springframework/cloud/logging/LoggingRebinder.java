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

package org.springframework.cloud.logging;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Listener that looks for {@link EnvironmentChangeEvent} and rebinds logger levels if any
 * changed.
 *
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 *
 */
public class LoggingRebinder
		implements ApplicationListener<EnvironmentChangeEvent>, EnvironmentAware {

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable
			.mapOf(String.class, String.class);

	private final Log logger = LogFactory.getLog(getClass());

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		if (this.environment == null) {
			return;
		}
		LoggingSystem system = LoggingSystem.get(LoggingSystem.class.getClassLoader());
		setLogLevels(system, this.environment);
	}

	protected void setLogLevels(LoggingSystem system, Environment environment) {
		Map<String, String> levels = Binder.get(environment)
				.bind("logging.level", STRING_STRING_MAP)
				.orElseGet(Collections::emptyMap);
		for (Entry<String, String> entry : levels.entrySet()) {
			setLogLevel(system, environment, entry.getKey(), entry.getValue().toString());
		}
	}

	private void setLogLevel(LoggingSystem system, Environment environment, String name,
			String level) {
		try {
			if (name.equalsIgnoreCase("root")) {
				name = null;
			}
			level = environment.resolvePlaceholders(level);
			system.setLogLevel(name, resolveLogLevel(level));
		}
		catch (RuntimeException ex) {
			this.logger.error("Cannot set level: " + level + " for '" + name + "'");
		}
	}

	private LogLevel resolveLogLevel(String level) {
		String trimmedLevel = level.trim();
		if ("false".equalsIgnoreCase(trimmedLevel)) {
			return LogLevel.OFF;
		}
		return LogLevel.valueOf(trimmedLevel.toUpperCase(Locale.ENGLISH));
	}

}
