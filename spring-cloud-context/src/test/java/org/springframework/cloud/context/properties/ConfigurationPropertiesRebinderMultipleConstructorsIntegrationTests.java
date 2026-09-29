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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderMultipleConstructorsIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that a {@code @ConfigurationProperties} bean which declares a no-argument
 * constructor <em>alongside</em> another constructor is rebound without discarding values
 * that were computed by the constructor actually used to create it (for example from a
 * collaborator), even though no property source carries those values. Reproduces
 * <a href="https://github.com/spring-cloud/spring-cloud-commons/issues/1733">gh-1733</a>,
 * where {@code EurekaInstanceConfigBean} - which declares a private no-arg constructor
 * next to the public one that computes {@code ipAddress} and {@code hostname} - had those
 * fields wiped out on every refresh.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class, properties = "test.message=Hello")
@ExtendWith(OutputCaptureExtension.class)
public class ConfigurationPropertiesRebinderMultipleConstructorsIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void rebindPreservesValueComputedByThePublicConstructor(CapturedOutput output) {
		then(this.properties.getMessage()).isEqualTo("Hello");
		then(this.properties.getComputed()).isEqualTo("computed-value");
		// Change a property that the bean does carry and rebind, exactly as an
		// EnvironmentChangeEvent-triggered refresh would.
		TestPropertyValues.of("test.message=World").applyTo(this.environment);
		this.rebinder.rebind();
		// Rebinding still applies the new value...
		then(this.properties.getMessage()).isEqualTo("World");
		// ...but does not wipe out a value no property source ever carried, just
		// because the bean also declares a no-arg constructor besides the one that
		// actually computed the value.
		then(this.properties.getComputed()).isEqualTo("computed-value");
		then(output).doesNotContain("Cannot create default instance");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties("computed-value");
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

		private String computed;

		private String message;

		// Declared for a framework's benefit only (for example bean deserialization);
		// it does not perform the computation that the constructor below does, which
		// is exactly the trap described in gh-1733.
		private TestProperties() {
		}

		public TestProperties(String computed) {
			this.computed = computed;
		}

		public String getComputed() {
			return this.computed;
		}

		public void setComputed(String computed) {
			this.computed = computed;
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

}
