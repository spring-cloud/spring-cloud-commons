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

package org.springframework.cloud.context.properties;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderTransientStateIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * A rebind must not expose the unbound class-level defaults of a bean to concurrent
 * readers: a nested object populated only by binding must never be set to {@code null},
 * and a scalar must never pass through its class-level default on the way to its bound
 * value (see gh-1727).
 */
@SpringBootTest(classes = TestConfiguration.class, properties = { "test.timeout.seconds=30", "test.workers=4" })
public class ConfigurationPropertiesRebinderTransientStateIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Test
	@DirtiesContext
	public void rebindDoesNotNullNestedObjectPopulatedByBinding() {
		then(this.properties.getTimeout().getSeconds()).isEqualTo(30);

		this.rebinder.rebind();

		then(this.properties.timeoutAssignments).doesNotContainNull();
		then(this.properties.getTimeout().getSeconds()).isEqualTo(30);
	}

	@Test
	@DirtiesContext
	public void rebindDoesNotSetScalarToClassLevelDefault() {
		then(this.properties.getWorkers()).isEqualTo(4);

		this.rebinder.rebind();

		then(this.properties.workerAssignments).containsOnly(4);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(RefreshAutoConfiguration.RefreshProperties.class)
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties();
		}

	}

	// Hack out a protected inner class for testing
	protected static class RefreshConfiguration extends RefreshAutoConfiguration {

		@Configuration(proxyBeanMethods = false)
		protected static class RebinderConfiguration extends ConfigurationPropertiesRebinderAutoConfiguration {

			public RebinderConfiguration(ApplicationContext context) {
				super(context);
			}

		}

	}

	@ConfigurationProperties("test")
	protected static class TestProperties {

		final List<Timeout> timeoutAssignments = new ArrayList<>();

		final List<Integer> workerAssignments = new ArrayList<>();

		private Timeout timeout;

		private int workers;

		public Timeout getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Timeout timeout) {
			this.timeoutAssignments.add(timeout);
			this.timeout = timeout;
		}

		public int getWorkers() {
			return this.workers;
		}

		public void setWorkers(int workers) {
			this.workerAssignments.add(workers);
			this.workers = workers;
		}

	}

	protected static class Timeout {

		private int seconds;

		public int getSeconds() {
			return this.seconds;
		}

		public void setSeconds(int seconds) {
			this.seconds = seconds;
		}

	}

}
