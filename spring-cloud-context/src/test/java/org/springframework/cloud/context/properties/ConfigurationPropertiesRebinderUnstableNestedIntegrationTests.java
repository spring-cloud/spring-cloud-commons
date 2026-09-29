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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderUnstableNestedIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.data.MapEntry.entry;

/**
 * Reproduces the three problems reported in gh-1750 against
 * {@code ConfigurationPropertiesRebinder.resetProperties}: a {@link StackOverflowError}
 * for read-only getters that return a new instance on every call, silently-skipped resets
 * for unmodifiable collections/maps, and silently-skipped resets for setters that reject
 * {@code null}.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class,
		properties = { "test.readonly.label.a=1", "test.readonly.label.b=2",
				"test.nullrejecting.endpoint=https://old.example.com" })
@ExtendWith(OutputCaptureExtension.class)
public class ConfigurationPropertiesRebinderUnstableNestedIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void rebindDoesNotStackOverflowForFluentAccessorReturningNewInstance() {
		// FluentProperties.getSettings() returns a new instance on every call, and
		// Settings.protect() does the same, so a naive recursive reset never terminates.
		// The rebind must complete rather than throw StackOverflowError.
		this.rebinder.rebind();
		then(this.properties.getFluent().getName()).isEqualTo("default");
	}

	@Test
	@DirtiesContext
	public void removedEntryLogsWarningForUnmodifiableView(CapturedOutput output) {
		then(this.properties.getReadonly().getLabels()).containsExactly(entry("a", "1"), entry("b", "2"));
		Map<String, Object> map = findTestProperties();
		map.remove("test.readonly.label.b");
		this.rebinder.rebind();
		// getLabels() only exposes an unmodifiable view over the private backing map, so
		// the
		// removed entry can't actually be cleared through the bean's public API - but the
		// failure must now be visible instead of silently swallowed at DEBUG.
		then(output).contains("Cannot reset property 'labels'");
	}

	@Test
	@DirtiesContext
	public void removedPropertyLogsWarningForNullRejectingSetter(CapturedOutput output) {
		then(this.properties.getNullrejecting().getEndpoint()).isEqualTo("https://old.example.com");
		Map<String, Object> map = findTestProperties();
		map.remove("test.nullrejecting.endpoint");
		this.rebinder.rebind();
		// The setter rejects null, so the stale value cannot be reset away, but the
		// failure must now be visible instead of silently swallowed at DEBUG.
		then(output).contains("Cannot reset property 'endpoint'");
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

		private final FluentProperties fluent = new FluentProperties();

		private final ReadOnlyViewProperties readonly = new ReadOnlyViewProperties();

		private final NullRejectingSetterProperties nullrejecting = new NullRejectingSetterProperties();

		public FluentProperties getFluent() {
			return this.fluent;
		}

		public ReadOnlyViewProperties getReadonly() {
			return this.readonly;
		}

		public NullRejectingSetterProperties getNullrejecting() {
			return this.nullrejecting;
		}

	}

	protected static class FluentProperties {

		private String name = "default";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		// Read-only: returns a new instance on every call.
		public Settings getSettings() {
			return new Settings();
		}

		protected static class Settings {

			private boolean protect;

			public boolean isProtect() {
				return this.protect;
			}

			// Record-style accessor (no "get" prefix, matches the "protect" field) that
			// returns a new, protected copy on every call - exactly like
			// org.infinispan.commons.configuration.attributes.AttributeSet#protect().
			public Settings protect() {
				Settings copy = new Settings();
				copy.protect = true;
				return copy;
			}

		}

	}

	protected static class ReadOnlyViewProperties {

		private final Map<String, String> labels = new LinkedHashMap<>();

		// Read-only: exposes an unmodifiable view over the mutable backing map.
		public Map<String, String> getLabels() {
			return Collections.unmodifiableMap(this.labels);
		}

		public void setLabel(Map<String, String> label) {
			this.labels.putAll(label);
		}

	}

	protected static class NullRejectingSetterProperties {

		private String endpoint;

		public String getEndpoint() {
			return this.endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint.strip();
		}

	}

}
