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

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderRefreshScopeIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class ConfigurationPropertiesRebinderRefreshScopeIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope refreshScope;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void testSimpleProperties() throws Exception {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...but don't refresh, so the bean stays the same:
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		then(this.properties.getCount()).isEqualTo(1);
	}

	@Test
	@DirtiesContext
	public void testRefresh() throws Exception {
		then(this.properties.getCount()).isEqualTo(1);
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		then(this.properties.getCount()).isEqualTo(1);
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...rebind, but the bean is not re-initialized:
		this.rebinder.rebind();
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		then(this.properties.getCount()).isEqualTo(1);
		// ...and then refresh, so the bean is re-initialized:
		this.refreshScope.refreshAll();
		then(this.properties.getMessage()).isEqualTo("Foo");
		// It's a new instance so the initialization count is 1
		then(this.properties.getCount()).isEqualTo(1);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshAutoConfiguration.class,
			ConfigurationPropertiesRebinderAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		@RefreshScope
		protected TestProperties properties() {
			return new TestProperties();
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

}
