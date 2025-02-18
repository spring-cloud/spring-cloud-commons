/*
 * Copyright 2012-2020 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class, properties = "spring.cloud.bootstrap.enabled=true")
@ActiveProfiles("config")
// TODO: fix and enable back
@Disabled
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
	public void testSimpleProperties() {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		then(this.properties.getCount()).isEqualTo(1);
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...but don't refresh, so the bean stays the same:
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		then(this.properties.getCount()).isEqualTo(1);
	}

	@Test
	@DirtiesContext
	public void testRefreshInParent() {
		then(this.config.getName()).isEqualTo("parent");
		// Change the dynamic property source...
		TestPropertyValues.of("config.name=foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind();
		then(this.config.getName()).isEqualTo("parent");
	}

	@Test
	@DirtiesContext
	public void testRefresh() {
		then(this.properties.getCount()).isEqualTo(1);
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind();
		then(this.properties.getMessage()).isEqualTo("Foo");
		then(this.properties.getCount()).isEqualTo(2);
	}

	@Test
	@DirtiesContext
	public void testRefreshByName() {
		then(this.properties.getCount()).isEqualTo(1);
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind("properties");
		then(this.properties.getMessage()).isEqualTo("Foo");
		then(this.properties.getCount()).isEqualTo(2);
	}

	interface SomeService {

		void foo();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@ImportAutoConfiguration({ RefreshConfiguration.RebinderConfiguration.class,
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

	// Hack out a protected inner class for testing
	protected static class RefreshConfiguration extends RefreshAutoConfiguration {

		@Configuration(proxyBeanMethods = false)
		protected static class RebinderConfiguration extends ConfigurationPropertiesRebinderAutoConfiguration {

		}

	}

	@ConfigurationProperties
	protected static class TestProperties implements InitializingBean {

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

		@Override
		public void afterPropertiesSet() {
			this.count++;
		}

	}

	@Configuration
	public static class ConfigPropertiesConfig {

		@Bean
		@ConditionalOnMissingBean(ConfigProperties.class)
		public ConfigProperties configProperties() {
			return new ConfigProperties();
		}

	}

	@ConfigurationProperties("config")
	public static class ConfigProperties {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
