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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderWrappedBeanIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that a {@code @ConfigurationProperties} bean that a {@link BeanPostProcessor}
 * replaces with an unrelated wrapper after its properties were bound (as Boot's
 * {@code spring.datasource.connection-fetch=lazy} does by wrapping the
 * {@code HikariDataSource} in a {@code LazyConnectionDataSourceProxy}) is rebound through
 * the instance the properties were bound to, not the wrapper.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class, properties = "test.message=Hello")
public class ConfigurationPropertiesRebinderWrappedBeanIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void rebindUsesBoundInstanceNotWrapper() {
		Wrapper wrapper = (Wrapper) this.context.getBean("testProperties");
		TestProperties target = wrapper.getTarget();
		then(target.getMessage()).isEqualTo("Hello");
		TestPropertyValues.of("test.message=World").applyTo(this.environment);

		this.rebinder.rebind();

		then(this.rebinder.getErrors()).isEmpty();
		// The wrapper is untouched and still points at the (rebound) target.
		then(this.context.getBean("testProperties")).isSameAs(wrapper);
		then(wrapper.getTarget()).isSameAs(target);
		then(target.getMessage()).isEqualTo("World");
		// A property that no source carries keeps the value set by the constructor.
		then(target.getComputed()).isEqualTo("computed-value");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties("computed-value");
		}

		@Bean
		protected static WrappingBeanPostProcessor wrappingBeanPostProcessor() {
			return new WrappingBeanPostProcessor();
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

	/**
	 * Replaces {@link TestProperties} with a {@link Wrapper} once it has been fully
	 * initialized, i.e. after the properties were bound.
	 */
	protected static class WrappingBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return (bean instanceof TestProperties properties) ? new Wrapper(properties) : bean;
		}

	}

	protected static class Wrapper {

		private final TestProperties target;

		Wrapper(TestProperties target) {
			this.target = target;
		}

		TestProperties getTarget() {
			return this.target;
		}

	}

	@ConfigurationProperties("test")
	protected static class TestProperties {

		private String computed;

		private String message;

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
