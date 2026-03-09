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
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderFieldInitializerIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests that field initializers in {@code @ConfigurationProperties} beans are restored
 * when properties are removed and the bean is rebound via a proxy.
 *
 * @see <a href=
 * "https://github.com/spring-cloud/spring-cloud-commons/issues/1616">gh-1616</a>
 */
@SpringBootTest(classes = TestConfiguration.class, properties = "my.name=overridden")
public class ConfigurationPropertiesRebinderFieldInitializerIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void fieldInitializerRestoredAfterPropertyRemoval() {
		// Initially the property overrides the field initializer
		then(this.properties.getName()).isEqualTo("overridden");
		then(this.properties.getTimeout()).isEqualTo(30);

		// Override timeout as well
		TestPropertyValues.of("my.timeout=60").applyTo(this.environment);
		this.rebinder.rebind();
		then(this.properties.getTimeout()).isEqualTo(60);

		// Remove all property sources that contain our overrides
		MutablePropertySources sources = this.environment.getPropertySources();
		sources.forEach(ps -> {
			if (ps.containsProperty("my.name") || ps.containsProperty("my.timeout")) {
				sources.remove(ps.getName());
			}
		});

		this.rebinder.rebind();

		// Field initializers should be restored
		then(this.properties.getName()).isEqualTo("default-name");
		then(this.properties.getTimeout()).isEqualTo(30);
	}

	@Test
	@DirtiesContext
	public void rebindStillWorksWithNewValues() {
		then(this.properties.getName()).isEqualTo("overridden");

		TestPropertyValues.of("my.name=updated").applyTo(this.environment);
		this.rebinder.rebind();

		then(this.properties.getName()).isEqualTo("updated");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ TestInterceptor.class, RefreshConfiguration.RebinderConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, AopAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties();
		}

	}

	@Aspect
	protected static class TestInterceptor {

		@Before("execution(* *..TestProperties.*(..))")
		public void before() {
			// Triggers AOP proxy creation for TestProperties
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

	@ConfigurationProperties("my")
	protected static class TestProperties {

		private String name = "default-name";

		private int timeout = 30;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getTimeout() {
			return this.timeout;
		}

		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}

	}

}
