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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerGroup;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Listener that looks for {@link EnvironmentChangeEvent} and rebinds logger levels if any
 * changed.
 *
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 * @author Haibo Wang
 *
 */
public class LoggingRebinder implements ApplicationListener<EnvironmentChangeEvent>, EnvironmentAware {

	private static Bindable<Map<String, List<String>>> STRING_STRINGS_MAP = Bindable
			.of(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class).asMap());

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

	private final Log logger = LogFactory.getLog(getClass());

	private Environment environment;

	final private static List<String> SPRING_LOGGING_GROUP_NAMES = Arrays.asList("web", "sql");

	private ArrayList<String> rootLoggersList = new ArrayList<>();

	final private LoggingSystem loggingSystem;

	final private LoggerGroups loggerGroups;

	public LoggingRebinder(LoggingSystem loggingSystem, LoggerGroups loggerGroups) {
		Assert.notNull(loggingSystem, "LoggingSystem must not be null");
		this.loggingSystem = loggingSystem;
		this.loggerGroups = loggerGroups;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		if (this.environment == null) {
			return;
		}

		if (this.loggerGroups != null) {
			List<String> deletedLoggerList = updateLoggerGroups(loggingSystem, environment, loggerGroups);

			for (String logger : deletedLoggerList) {
				if (!rootLoggersList.contains(logger)) {
					rootLoggersList.add(logger);
				}
			}
		}

		setDefaultLogLevel(rootLoggersList, loggingSystem, environment);
		setLogLevels(loggingSystem, environment, loggerGroups);
	}

	/**
	 * Merges the definition of a logger group from the configuration and returns the
	 * loggers that have been removed from the logger group.
	 * @param loggingSystem the logging system
	 * @param environment the environment
	 * @param loggerGroups the logger groups
	 * @return A list of logger that have been removed from the log group
	 */
	protected List<String> updateLoggerGroups(LoggingSystem loggingSystem, Environment environment,
			LoggerGroups loggerGroups) {
		Map<String, List<String>> loggerGroupsMap = Binder.get(environment).bind("logging.group", STRING_STRINGS_MAP)
				.orElseGet(Collections::emptyMap);

		ArrayList<String> deletedList = new ArrayList<String>();

		// The deleted logger group
		Map<String, List<String>> deletedGroup = new HashMap<String, List<String>>();
		for (LoggerGroup loggerGroup : loggerGroups) {
			if (!loggerGroupsMap.containsKey(loggerGroup.getName())
					&& !SPRING_LOGGING_GROUP_NAMES.contains(loggerGroup.getName())) {
				for (String loggerName : loggerGroup.getMembers()) {
					if (!deletedList.contains(loggerName)) {
						deletedList.add(loggerName);
					}
				}
				deletedGroup.put(loggerGroup.getName(), Collections.emptyList());
			}
		}
		loggerGroups.putAll(deletedGroup);

		for (Entry<String, List<String>> entry : loggerGroupsMap.entrySet()) {
			LoggerGroup loggerGroup = loggerGroups.get(entry.getKey());
			if (loggerGroup != null) {
				for (String loggerName : loggerGroup.getMembers()) {
					if (!entry.getValue().contains(loggerName)) {
						if (!deletedList.contains(loggerName)) {
							deletedList.add(loggerName);
						}
					}
				}
			}
		}
		loggerGroups.putAll(loggerGroupsMap);

		return deletedList;
	}

	protected void setLogLevels(LoggingSystem system, Environment environment, LoggerGroups loggerGroups) {
		Map<String, String> levels = Binder.get(environment).bind("logging.level", STRING_STRING_MAP)
				.orElseGet(Collections::emptyMap);
		for (Entry<String, String> entry : levels.entrySet()) {
			setLogLevel(loggingSystem, environment, loggerGroups, entry.getKey(), entry.getValue());
		}
	}

	private void setLogLevel(LoggingSystem system, Environment environment, LoggerGroups loggerGroups, String name,
			String level) {
		try {
			level = environment.resolvePlaceholders(level);
			LogLevel logLevel = resolveLogLevel(level);

			if (loggerGroups != null) {
				LoggerGroup loggerGroup = loggerGroups.get(name);
				if (loggerGroup != null && loggerGroup.hasMembers()) {
					loggerGroup.configureLogLevel(logLevel, this::setLogLevel);
				}
			}

			if ("root".equalsIgnoreCase(name)) {
				name = null;
			}

			setLogLevel(name, logLevel);
		}
		catch (RuntimeException ex) {
			this.logger.error("Cannot set level: " + level + " for '" + name + "'");
		}
	}

	private void setLogLevel(String name, LogLevel logLevel) {
		loggingSystem.setLogLevel(name, logLevel);
	}

	private void setDefaultLogLevel(List<String> loggerNames, LoggingSystem loggingSystem, Environment environment) {
		if (loggerNames.isEmpty()) {
			return;
		}

		LogLevel logLevel = determineLogLevel(environment);
		for (String loggerName : loggerNames) {
			setLogLevel(loggerName, logLevel);
		}
	}

	private LogLevel determineLogLevel(Environment environment) {
		LogLevel logLevel = LogLevel.INFO;
		String level = environment.getProperty("logging.level.root");
		if (StringUtils.hasLength(level)) {
			logLevel = resolveLogLevel(level);
		}
		else {
			Log log = LogFactory.getLog(LoggingSystem.ROOT_LOGGER_NAME);
			if (log.isTraceEnabled()) {
				logLevel = LogLevel.TRACE;
			}
			else if (log.isDebugEnabled()) {
				logLevel = LogLevel.DEBUG;
			}
			else if (log.isInfoEnabled()) {
				logLevel = LogLevel.INFO;
			}
			else if (log.isErrorEnabled()) {
				logLevel = LogLevel.ERROR;
			}
			else if (log.isFatalEnabled()) {
				logLevel = LogLevel.FATAL;
			}
		}

		return logLevel;
	}

	private LogLevel resolveLogLevel(String level) {
		String trimmedLevel = level.trim();
		if ("false".equalsIgnoreCase(trimmedLevel)) {
			return LogLevel.OFF;
		}
		return LogLevel.valueOf(trimmedLevel.toUpperCase(Locale.ENGLISH));
	}

}
