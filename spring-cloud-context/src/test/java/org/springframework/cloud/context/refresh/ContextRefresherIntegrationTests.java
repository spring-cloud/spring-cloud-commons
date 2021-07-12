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

package org.springframework.cloud.context.refresh;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.context.refresh.ContextRefresherIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class,
		properties = { "spring.datasource.hikari.read-only=false", "spring.config.use-legacy-processing=true" })
public class ContextRefresherIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private ConfigurableEnvironment environment;

	@Autowired
	private ContextRefresher refresher;

	@Test
	@DirtiesContext
	public void testSimpleProperties() throws Exception {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...but don't refresh, so the bean stays the same:
		then(this.properties.getMessage()).isEqualTo("Foo");
	}

	@Test
	@DirtiesContext
	public void testRefreshBean() throws Exception {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...and then refresh, so the bean is re-initialized:
		this.refresher.refresh();
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
	}

	@Test
	@DirtiesContext
	public void testUpdateHikari() throws Exception {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		TestPropertyValues.of("spring.datasource.hikari.read-only=true").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.refresher.refresh();
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
	}

	@Test
	@DirtiesContext
	public void testCachedRandom() {
		long cachedRandomLong = properties.getCachedRandomLong();
		long randomLong = properties.randomLong();
		then(cachedRandomLong).isNotNull();
		this.refresher.refresh();
		then(randomLong).isNotEqualTo(properties.randomLong());
		then(cachedRandomLong).isEqualTo(properties.cachedRandomLong);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(TestProperties.class)
	@EnableAutoConfiguration
	protected static class TestConfiguration {

	}

	@ConfigurationProperties
	@ManagedResource
	protected static class TestProperties {

		private String message;

		private int delay;

		private Long cachedRandomLong;

		private Long randomLong;

		@ManagedAttribute
		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@ManagedAttribute
		public int getDelay() {
			return this.delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

		public long getCachedRandomLong() {
			return cachedRandomLong;
		}

		public void setCachedRandomLong(long cachedRandomLong) {
			this.cachedRandomLong = cachedRandomLong;
		}

		public long randomLong() {
			return randomLong;
		}

		public void setRandomLong(long randomLong) {
			this.randomLong = randomLong;
		}

	}

}
