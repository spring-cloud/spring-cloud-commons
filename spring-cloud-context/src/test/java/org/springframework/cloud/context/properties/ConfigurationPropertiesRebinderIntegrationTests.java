/*
 * Copyright 2006-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.context.properties;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@ActiveProfiles("config")
public class ConfigurationPropertiesRebinderIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigProperties config;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void testSimpleProperties() throws Exception {
		assertEquals("Hello scope!", this.properties.getMessage());
		assertEquals(1, this.properties.getCount());
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...but don't refresh, so the bean stays the same:
		assertEquals("Hello scope!", this.properties.getMessage());
		assertEquals(1, this.properties.getCount());
	}

	@Test
	@DirtiesContext
	public void testRefreshInParent() throws Exception {
		assertEquals("main", this.config.getName());
		// Change the dynamic property source...
		TestPropertyValues.of("config.name=foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind();
		assertEquals("foo", this.config.getName());
	}

	@Test
	@DirtiesContext
	public void testRefresh() throws Exception {
		assertEquals(1, this.properties.getCount());
		assertEquals("Hello scope!", this.properties.getMessage());
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind();
		assertEquals("Foo", this.properties.getMessage());
		assertEquals(2, this.properties.getCount());
	}

	@Test
	@DirtiesContext
	public void testRefreshByName() throws Exception {
		assertEquals(1, this.properties.getCount());
		assertEquals("Hello scope!", this.properties.getMessage());
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind("properties");
		assertEquals("Foo", this.properties.getMessage());
		assertEquals(2, this.properties.getCount());
	}

	@Configuration
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties properties() {
			return new TestProperties();
		}

		// exposes https://github.com/spring-cloud/spring-cloud-commons/issues/337
		@Bean
		@ConfigurationProperties("some.service")
		public SomeService someService() {
			return ProxyFactory.getProxy(SomeService.class, (MethodInterceptor) methodInvocation -> null);
		}
	}

	interface SomeService {
		void foo();
	}

	// Hack out a protected inner class for testing
	protected static class RefreshConfiguration extends RefreshAutoConfiguration {
		@Configuration
		protected static class RebinderConfiguration
				extends ConfigurationPropertiesRebinderAutoConfiguration {

		}
	}

	@ConfigurationProperties
	protected static class TestProperties {
		private String message;
		private int delay;
		private int count = 0;

		public int getCount() {
			return this.count;
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getDelay() {
			return this.delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

		@PostConstruct
		public void init() {
			this.count++;
		}
	}

	@ConfigurationProperties("config")
	@ConditionalOnMissingBean(ConfigProperties.class)
	public static class ConfigProperties {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
