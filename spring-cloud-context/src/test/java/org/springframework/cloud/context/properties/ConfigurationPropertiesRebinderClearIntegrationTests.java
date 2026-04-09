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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderClearIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class, properties = { "test.message=Hello", "test.count=5" })
public class ConfigurationPropertiesRebinderClearIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void testPropertyClearedToNull() {
		then(this.properties.getMessage()).isEqualTo("Hello");
		// Remove the property from the environment
		Map<String, Object> map = findTestProperties();
		map.remove("test.message");
		// Rebind
		this.rebinder.rebind();
		// The property should be cleared to its default (null)
		then(this.properties.getMessage()).isNull();
	}

	@Test
	@DirtiesContext
	public void testPrimitivePropertyClearedToDefault() {
		then(this.properties.getCount()).isEqualTo(5);
		// Remove the property from the environment
		Map<String, Object> map = findTestProperties();
		map.remove("test.count");
		// Rebind
		this.rebinder.rebind();
		// The primitive property should be cleared to its class-level default (0)
		then(this.properties.getCount()).isEqualTo(0);
	}

	@Test
	@DirtiesContext
	public void testPropertyWithFieldDefaultRestoredOnRemoval() {
		then(this.properties.getName()).isEqualTo("default-name");
		// Set a value
		TestPropertyValues.of("test.name=custom").applyTo(this.environment);
		this.rebinder.rebind();
		then(this.properties.getName()).isEqualTo("custom");
		// Remove the property
		Map<String, Object> map = findTestProperties();
		map.remove("test.name");
		this.rebinder.rebind();
		// Should revert to field initializer default
		then(this.properties.getName()).isEqualTo("default-name");
	}

	@Test
	@DirtiesContext
	public void testPropertyChangedToNewValue() {
		then(this.properties.getMessage()).isEqualTo("Hello");
		// Change the property
		TestPropertyValues.of("test.message=World").applyTo(this.environment);
		this.rebinder.rebind();
		then(this.properties.getMessage()).isEqualTo("World");
	}

	private Map<String, Object> findTestProperties() {
		for (PropertySource<?> source : this.environment.getPropertySources()) {
			if (source.getName().toLowerCase().contains("test")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) source.getSource();
				return map;
			}
		}
		throw new IllegalStateException("Could not find test property source");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
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

		private String message;

		private int count;

		private String name = "default-name";

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getCount() {
			return this.count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
