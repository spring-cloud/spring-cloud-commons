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
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderResetExclusionIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that types listed in {@code spring.cloud.refresh.never-reset-nested-types} are
 * not recursively reset during rebind.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class, properties = { "test.nested.host=custom-host",
		"spring.cloud.refresh.never-reset-nested-types=org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderResetExclusionIntegrationTests$NestedProperties" })
public class ConfigurationPropertiesRebinderResetExclusionIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void excludedNestedTypeIsNotReset() {
		then(this.properties.getNested().getHost()).isEqualTo("custom-host");
		// Remove the nested property and rebind
		Map<String, Object> map = findTestProperties();
		map.remove("test.nested.host");
		this.rebinder.rebind();
		// Because the nested type is excluded, it is not descended into and its value is
		// left untouched rather than being reset to the field default.
		then(this.properties.getNested().getHost()).isEqualTo("custom-host");
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

		private final NestedProperties nested = new NestedProperties();

		public NestedProperties getNested() {
			return this.nested;
		}

	}

	protected static class NestedProperties {

		private String host = "default-host";

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

	}

}
