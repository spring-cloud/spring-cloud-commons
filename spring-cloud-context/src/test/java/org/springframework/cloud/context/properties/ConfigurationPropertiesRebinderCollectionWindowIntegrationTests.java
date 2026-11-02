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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderCollectionWindowIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that resetting getter-only collection and map properties to their defaults
 * never exposes them empty to a concurrent reader (gh-1727). Such properties cannot be
 * replaced through a setter, so they are updated in place and must not be cleared and
 * refilled.
 * <p>
 * Only the removal of a property is exercised. While a value is present in the
 * Environment, Spring Boot's own binder clears and refills a getter-only collection it
 * binds, which is outside the control of the rebinder.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class)
public class ConfigurationPropertiesRebinderCollectionWindowIntegrationTests {

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void collectionsAndMapsAreNeverEmptyWhenResetToDefaults() throws Exception {
		AtomicBoolean checking = new AtomicBoolean();
		AtomicInteger emptyReads = new AtomicInteger();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread reader = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					if (checking.get() && (this.properties.getItems().isEmpty() || this.properties.getTags().isEmpty()
							|| this.properties.getAttrs().isEmpty())) {
						emptyReads.incrementAndGet();
					}
				}
			}
			catch (Throwable ex) {
				failure.set(ex);
			}
		});
		reader.start();
		try {
			for (int i = 0; i < 300; i++) {
				TestPropertyValues.of("test.items=a,b,c", "test.tags=x,y", "test.attrs.k1=v1", "test.attrs.k2=v2")
					.applyTo(this.environment);
				this.rebinder.rebind("testProperties");
				// Removing the values resets the collections to their defaults
				this.environment.getPropertySources().remove("test");
				checking.set(true);
				this.rebinder.rebind("testProperties");
				checking.set(false);
			}
		}
		finally {
			reader.interrupt();
			reader.join(10_000);
		}
		then(failure.get()).isNull();
		then(emptyReads.get()).as("reads that saw an empty collection or map").isZero();
		then(this.properties.getItems()).containsExactly("d");
		then(this.properties.getTags()).containsExactly("t");
		then(this.properties.getAttrs()).containsOnlyKeys("dk");
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

		// getter-only: no setters, so these can only be updated in place
		private final List<String> items = new ArrayList<>(List.of("d"));

		private final Set<String> tags = new HashSet<>(Set.of("t"));

		private final Map<String, String> attrs = new LinkedHashMap<>(Map.of("dk", "dv"));

		public List<String> getItems() {
			return this.items;
		}

		public Set<String> getTags() {
			return this.tags;
		}

		public Map<String, String> getAttrs() {
			return this.attrs;
		}

	}

}
