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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderTransientStateIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * A rebind must never pass the live bean through its unbound class-level defaults, which
 * is what a concurrent reader would observe (see gh-1727): a nested object populated only
 * by binding must never be set to {@code null}, a scalar must never be set to its
 * class-level default on the way to its bound value, and a rebind whose binding fails
 * must leave the bean as it was, and a getter-only collection or map must never be
 * emptied on the way to its new contents. The tests check the values the bean's setters
 * receive during a rebind and the operations its collections see.
 */
@SpringBootTest(classes = TestConfiguration.class, properties = { "test.timeout.seconds=30", "test.workers=4",
		"test.tags[0]=a", "test.tags[1]=b", "test.labels.one=1", "test.labels.two=2" })
public class ConfigurationPropertiesRebinderTransientStateIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

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

	@Test
	@DirtiesContext
	public void failedRebindLeavesTheBeanUntouched() {
		then(this.properties.getWorkers()).isEqualTo(4);
		findTestProperties().put("test.workers", "not-a-number");

		thenThrownBy(() -> this.rebinder.rebind()).isInstanceOf(ConfigurationPropertiesBindException.class);

		then(this.properties.workerAssignments).containsOnly(4);
		then(this.properties.getWorkers()).isEqualTo(4);
		then(this.properties.getTimeout().getSeconds()).isEqualTo(30);
	}

	@Test
	@DirtiesContext
	public void rebindBringsGetterOnlyMapToNewContentsWithoutEmptyingIt() {
		then(this.properties.getLabels()).containsOnly(entry("one", "1"), entry("two", "2"));
		findTestProperties().remove("test.labels.two");
		findTestProperties().put("test.labels.three", "3");
		this.rebinder.rebind();
		then(this.properties.getLabels()).containsOnly(entry("one", "1"), entry("three", "3"));
		then(this.properties.getLabels().clears).isZero();
	}

	@Test
	@DirtiesContext
	public void rebindBringsGetterOnlyListToNewContents() {
		then(this.properties.getTags()).containsExactly("a", "b");
		findTestProperties().put("test.tags[0]", "c");
		findTestProperties().remove("test.tags[1]");
		this.rebinder.rebind();
		then(this.properties.getTags()).containsExactly("c");
	}

	@Test
	@DirtiesContext
	public void rebindEmptiesRemovedGetterOnlyList() {
		then(this.properties.getTags()).containsExactly("a", "b");
		findTestProperties().remove("test.tags[0]");
		findTestProperties().remove("test.tags[1]");
		this.rebinder.rebind();
		then(this.properties.getTags()).isEmpty();
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

		final List<Timeout> timeoutAssignments = new ArrayList<>();

		final List<Integer> workerAssignments = new ArrayList<>();

		private final List<String> tags = new ArrayList<>();

		private final TrackingMap labels = new TrackingMap();

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

		public List<String> getTags() {
			return this.tags;
		}

		public TrackingMap getLabels() {
			return this.labels;
		}

	}

	@SuppressWarnings("serial")
	protected static class TrackingMap extends LinkedHashMap<String, String> {

		int clears;

		@Override
		public void clear() {
			this.clears++;
			super.clear();
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
