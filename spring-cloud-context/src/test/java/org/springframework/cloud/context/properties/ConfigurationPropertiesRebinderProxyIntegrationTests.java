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

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderProxyIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class, properties = { "messages.expiry.one=168", "messages.expiry.two=76",
		"messages.name=custom", "messages.count=5" })
public class ConfigurationPropertiesRebinderProxyIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void testAppendProperties() {
		// This comes out as a String not Integer if the rebinder processes the proxy
		// instead of the target
		then(this.properties.getExpiry().get("one")).isEqualTo(new Integer(168));
		TestPropertyValues.of("messages.expiry.one=56").applyTo(this.environment);
		this.rebinder.rebind();
		then(this.properties.getExpiry().get("one")).isEqualTo(new Integer(56));
	}

	@Test
	@DirtiesContext
	public void testPropertyClearedToNullThroughProxy() {
		then(this.properties.getName()).isEqualTo("custom");
		Map<String, Object> map = findTestProperties();
		map.remove("messages.name");
		this.rebinder.rebind();
		then(this.properties.getName()).isNull();
	}

	@Test
	@DirtiesContext
	public void testPrimitivePropertyClearedToDefaultThroughProxy() {
		then(this.properties.getCount()).isEqualTo(5);
		Map<String, Object> map = findTestProperties();
		map.remove("messages.count");
		this.rebinder.rebind();
		then(this.properties.getCount()).isEqualTo(0);
	}

	@Test
	@DirtiesContext
	public void testFieldInitializerRestoredThroughProxy() {
		then(this.properties.getLabel()).isEqualTo("default-label");
		// Set a value
		TestPropertyValues.of("messages.label=custom").applyTo(this.environment);
		this.rebinder.rebind();
		then(this.properties.getLabel()).isEqualTo("custom");
		// Remove the property
		Map<String, Object> map = findTestProperties();
		map.remove("messages.label");
		this.rebinder.rebind();
		// Should revert to field initializer default
		then(this.properties.getLabel()).isEqualTo("default-label");
	}

	private Map<String, Object> findTestProperties() {
		for (PropertySource<?> source : this.environment.getPropertySources()) {
			if (source.getName().toLowerCase().contains("test")) {
				Object sourceObj = source.getSource();
				if (sourceObj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>) sourceObj;
					return map;
				}
			}
		}
		throw new IllegalStateException("Could not find test property source");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ Interceptor.class, RefreshConfiguration.RebinderConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, AopAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties properties() {
			return new TestProperties();
		}

	}

	@Aspect
	protected static class Interceptor {

		@Before("execution(* *..TestProperties.*(..))")
		public void before() {
			System.err.println("Before");
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

	@ConfigurationProperties("messages")
	protected static class TestProperties {

		private final Map<String, Integer> expiry = new HashMap<>();

		private String name;

		private int count;

		private String label = "default-label";

		public Map<String, Integer> getExpiry() {
			return this.expiry;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getCount() {
			return this.count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String getLabel() {
			return this.label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

	}

}
