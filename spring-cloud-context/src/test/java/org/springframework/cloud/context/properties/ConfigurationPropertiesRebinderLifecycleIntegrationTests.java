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

import org.assertj.core.api.AtomicBooleanAssert;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderLifecycleIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class)
public class ConfigurationPropertiesRebinderLifecycleIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void racy_properties(boolean skipReset) throws Exception {
		ConfigurationPropertiesRebinder.skipReset = skipReset;
		int violations = 0;
		var run = new AtomicBoolean(true);

		// message is not null at start of test
		then(this.properties.getMessage()).isEqualTo("Hello scope!");

		// run the test for 1 second
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				run.set(false);
			}
		}).start();

		// a thread calls rebind() in a loop
		new Thread(() -> {
			while (run.get()) {
				rebinder.rebind();
			}
		}).start();

		// read the properties in a loop
		while (run.get()) {
			String message = properties.getMessage();
			// we expect that message is never null as we don't change the properties
			if (message == null) {
				violations++;
			}
		}
		assertThat(violations).isEqualTo(0);
	}

	@Test
	@DirtiesContext
	public void testRefresh() {
		then(this.properties.getCount()).isEqualTo(0);
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.rebinder.rebind();
		then(this.properties.getMessage()).isEqualTo("Foo");
		then(this.properties.getCount()).isEqualTo(1);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties properties() {
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

	@ConfigurationProperties
	protected static class TestProperties implements DisposableBean, InitializingBean {

		private String message;

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

		@Override
		public void afterPropertiesSet() throws Exception {
			then(this.message).isNotEmpty();
		}

		@Override
		public void destroy() throws Exception {
			this.message = "";
			this.count++;
		}

	}

}
