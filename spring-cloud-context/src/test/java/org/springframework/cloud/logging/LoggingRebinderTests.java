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

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 *
 */
public class LoggingRebinderTests {

	private LoggingRebinder rebinder = new LoggingRebinder();

	private Logger logger = LoggerFactory.getLogger("org.springframework.web");

	@AfterEach
	public void reset() {
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("org.springframework.web", LogLevel.INFO);
	}

	@Test
	public void logLevelsChanged() {
		then(this.logger.isTraceEnabled()).isFalse();
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("logging.level.org.springframework.web=TRACE").applyTo(environment);
		this.rebinder.setEnvironment(environment);
		this.rebinder.onApplicationEvent(new EnvironmentChangeEvent(environment,
				Collections.singleton("logging.level.org.springframework.web")));
		then(this.logger.isTraceEnabled()).isTrue();
	}

	@Test
	public void logLevelsLowerCase() {
		then(this.logger.isTraceEnabled()).isFalse();
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("logging.level.org.springframework.web=trace").applyTo(environment);
		this.rebinder.setEnvironment(environment);
		this.rebinder.onApplicationEvent(new EnvironmentChangeEvent(environment,
				Collections.singleton("logging.level.org.springframework.web")));
		then(this.logger.isTraceEnabled()).isTrue();
	}

	@Test
	public void logLevelFalseResolvedToOff() {
		ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger("org.springframework.cloud");
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("logging.level.org.springframework.cloud=false").applyTo(environment);
		rebinder.setEnvironment(environment);
		rebinder.onApplicationEvent(new EnvironmentChangeEvent(environment,
				Collections.singleton("logging.level.org.springframework.cloud")));
		then(Level.OFF).isEqualTo((logger.getLevel()));
	}

}
