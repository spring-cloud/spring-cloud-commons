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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.context.test.TestConfigDataLocationResolver;
import org.springframework.cloud.context.test.TestEnvPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.BDDAssertions.then;

public class ConfigDataContextRefresherIntegrationTests {

	private TestProperties properties;

	private ConfigurableEnvironment environment;

	private ContextRefresher refresher;

	private ConfigurableApplicationContext context;

	@BeforeEach
	public void setup() {
		TestConfigDataLocationResolver.instance = new MyTestBean();
		System.setProperty("VCAP_SERVICES",
				"{\"user-provided\":[{\"label\": \"user-provided\",\"name\": \"myvcap\",\"myvar\": \"myval\"}]}");
		context = new SpringApplication(TestConfiguration.class).run("--spring.datasource.hikari.read-only=false",
				"--spring.profiles.active=configdatarefresh", "--" + TestEnvPostProcessor.EPP_ENABLED + "=true",
				"--server.port=0");
		properties = context.getBean(TestProperties.class);
		environment = context.getBean(ConfigurableEnvironment.class);
		refresher = context.getBean(ContextRefresher.class);
	}

	@AfterEach
	public void after() {
		System.clearProperty("VCAP_SERVICES");
		if (context != null) {
			context.close();
		}
		TestConfigDataLocationResolver.count.set(1);
		TestConfigDataLocationResolver.instance = null;
	}

	@Test
	public void testSimpleProperties() {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...but don't refresh, so the bean stays the same:
		then(this.properties.getMessage()).isEqualTo("Foo");
	}

	@Test
	public void testAdditionalPropertySourcesToRetain() {
		then(environment.getProperty(TestEnvPostProcessor.EPP_VALUE)).isEqualTo("1");
		// ...and then refresh, to see if property source is retained during refresh
		// that means an updated test datasource with EPP_VALUE set to 10
		TestConfigDataLocationResolver.count.set(10);
		this.refresher.refresh();
		then(environment.getProperty(TestEnvPostProcessor.EPP_VALUE)).isEqualTo("10");
	}

	@Test
	public void testRefreshBean() {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...and then refresh, so the bean is re-initialized:
		this.refresher.refresh();
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
	}

	@Test
	public void testVcapPlaceholderAfterRefresh() {
		// an error will be thrown if count is 99 and myplaceholder contains ${vcap
		TestConfigDataLocationResolver.count.set(99);
		this.refresher.refresh();
	}

	@Test
	public void testUpdateHikari() {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		TestPropertyValues.of("spring.datasource.hikari.read-only=true").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.refresher.refresh();
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
	}

	@Test
	public void testCachedRandom() {
		long cachedRandomLong = properties.getCachedRandomLong();
		long randomLong = properties.randomLong();
		then(cachedRandomLong).isNotNull();
		this.refresher.refresh();
		then(randomLong).isNotEqualTo(properties.randomLong());
		then(cachedRandomLong).isEqualTo(properties.cachedRandomLong);
	}

	@Test
	public void contextContainsBootstrapContext() {
		ConfigurableBootstrapContext bootstrapContext = context.getBean(ConfigurableBootstrapContext.class);
		then(bootstrapContext).isNotNull();
		then(bootstrapContext.isRegistered(MyTestBean.class)).isTrue();
	}

	protected static class MyTestBean {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(TestProperties.class)
	@EnableAutoConfiguration
	protected static class TestConfiguration {

	}

	@ConfigurationProperties
	protected static class TestProperties {

		private String message;

		private int delay;

		private Long cachedRandomLong;

		private Long randomLong;

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

		public Long getCachedRandomLong() {
			return this.cachedRandomLong;
		}

		public void setCachedRandomLong(Long cachedRandomLong) {
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
