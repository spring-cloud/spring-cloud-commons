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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.BDDAssertions.then;

public class ConfigDataContextRefresherIntegrationTests {

	private static final String EPP_VALUE = "configdatarefresh.epp.count";

	private static final String EPP_ENABLED = "configdatarefresh.epp.enabled";

	private TestProperties properties;

	private ConfigurableEnvironment environment;

	private ContextRefresher refresher;

	private ConfigurableApplicationContext context;

	@BeforeEach
	public void setup() {
		context = new SpringApplication(TestConfiguration.class).run("--spring.datasource.hikari.read-only=false",
				"--spring.profiles.active=configdatarefresh", "--" + EPP_ENABLED + "=true", "--server.port=0");
		properties = context.getBean(TestProperties.class);
		environment = context.getBean(ConfigurableEnvironment.class);
		refresher = context.getBean(ContextRefresher.class);
	}

	@AfterEach
	public void after() {
		if (context != null) {
			context.close();
		}
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
		then(environment.getProperty(EPP_VALUE)).isEqualTo("1");
		// ...and then refresh, to see if property source is retained during refresh
		// that means an updated test datasource with EPP_VALUE set to 10
		TestConfigDataLocationResolver.count.set(10);
		this.refresher.refresh();
		then(environment.getProperty(EPP_VALUE)).isEqualTo("10");
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

	protected static class TestEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
			if (environment.getProperty(EPP_ENABLED, Boolean.class, false)) {
				Map<String, Object> source = new HashMap<>();
				source.put("spring.cloud.refresh.additional-property-sources-to-retain", getClass().getSimpleName());
				source.put("spring.config.import", "testdatasource:");
				MapPropertySource propertySource = new MapPropertySource(getClass().getSimpleName(), source);
				environment.getPropertySources().addFirst(propertySource);
			}
		}

		@Override
		public int getOrder() {
			return ConfigDataEnvironmentPostProcessor.ORDER - 1;
		}

	}

	protected static class TestConfigDataResource extends ConfigDataResource {

		private final int count;

		public TestConfigDataResource(int count) {
			this.count = count;
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestConfigDataResource that = (TestConfigDataResource) o;
			return Objects.equals(this.count, that.count);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.count);
		}

	}

	protected static class TestConfigDataLocationResolver
			implements ConfigDataLocationResolver<TestConfigDataResource> {

		static AtomicInteger count = new AtomicInteger(1);

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			return location.hasPrefix("testdatasource:");
		}

		@Override
		public List<TestConfigDataResource> resolve(ConfigDataLocationResolverContext context,
				ConfigDataLocation location)
				throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
			return Collections.singletonList(new TestConfigDataResource(count.get()));
		}

	}

	protected static class TestConfigDataLoader implements ConfigDataLoader<TestConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource resource)
				throws ConfigDataResourceNotFoundException {
			Map<String, Object> stringStringMap = Collections.singletonMap(EPP_VALUE, resource.count);
			return new ConfigData(
					Collections.singletonList(new MapPropertySource("testconfigdatadatasource", stringStringMap)));
		}

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
