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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.bootstrap.TestBootstrapConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

public class ContextRefresherTests {

	private RefreshScope scope = Mockito.mock(RefreshScope.class);

	@AfterEach
	public void close() {
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
		TestLoggingSystem.count = 0;
	}

	@Test
	@Disabled // FIXME: legacy config
	public void orderNewPropertiesConsistentWithNewContext() {
		try (ConfigurableApplicationContext context = SpringApplication.run(Empty.class,
				"--spring.config.use-legacy-processing=true", "--spring.main.web-application-type=none",
				"--debug=false", "--spring.main.bannerMode=OFF")) {
			context.getEnvironment().setActiveProfiles("refresh");
			List<String> names = names(context.getEnvironment().getPropertySources());
			then(names).doesNotContain("applicationConfig: [classpath:/bootstrap-refresh.properties]");
			LegacyContextRefresher refresher = new LegacyContextRefresher(context, this.scope);
			refresher.refresh();
			names = names(context.getEnvironment().getPropertySources());
			then(names).contains("applicationConfig: [classpath:/bootstrap-refresh.properties]");
			then(names).containsSequence("applicationConfig: [classpath:/application.properties]",
					"applicationConfig: [classpath:/bootstrap-refresh.properties]",
					"applicationConfig: [classpath:/bootstrap.properties]");
		}
	}

	@Test
	public void bootstrapPropertySourceAlwaysFirst() {
		// Use spring.cloud.bootstrap.name to switch off the defaults (which would pick up
		// a bootstrapProperties immediately
		try (ConfigurableApplicationContext context = SpringApplication.run(Empty.class,
				"--spring.config.use-legacy-processing=true", "--spring.main.web-application-type=none",
				"--debug=false", "--spring.main.bannerMode=OFF", "--spring.cloud.bootstrap.name=refresh")) {
			List<String> names = names(context.getEnvironment().getPropertySources());
			System.err.println("***** " + context.getEnvironment().getPropertySources());
			then(names).doesNotContain("bootstrapProperties");
			ContextRefresher refresher = new LegacyContextRefresher(context, this.scope);
			TestPropertyValues.of("spring.cloud.bootstrap.sources: "
					+ "org.springframework.cloud.context.refresh.ContextRefresherTests.PropertySourceConfiguration")
					.applyTo(context.getEnvironment(), Type.MAP, "defaultProperties");
			refresher.refresh();
			names = names(context.getEnvironment().getPropertySources());
			then(names).first()
					.isEqualTo(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-refreshTest");
		}
	}

	@Test
	public void parentContextIsClosed() {
		// Use spring.cloud.bootstrap.name to switch off the defaults (which would pick up
		// a bootstrapProperties immediately
		try (ConfigurableApplicationContext context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.web-application-type=none", "--spring.config.use-legacy-processing=true",
				"--debug=false", "--spring.main.bannerMode=OFF", "--spring.cloud.bootstrap.name=refresh")) {
			LegacyContextRefresher refresher = new LegacyContextRefresher(context, this.scope);
			TestPropertyValues.of("spring.cloud.bootstrap.sources: "
					+ "org.springframework.cloud.context.refresh.ContextRefresherTests.PropertySourceConfiguration")
					.applyTo(context);

			ConfigurableApplicationContext refresherContext = refresher.addConfigFilesToEnvironment();
			then(refresherContext.getParent()).isNotNull().isInstanceOf(ConfigurableApplicationContext.class);
			ConfigurableApplicationContext parent = (ConfigurableApplicationContext) refresherContext.getParent();
			then(parent.isActive()).isFalse();
		}
	}

	@Test
	public void loggingSystemNotInitialized() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestLoggingSystem.class.getName());
		TestLoggingSystem system = (TestLoggingSystem) LoggingSystem.get(getClass().getClassLoader());
		then(system.getCount()).isEqualTo(0);
		try (ConfigurableApplicationContext context = SpringApplication.run(Empty.class,
				"--spring.config.use-legacy-processing=true", "--spring.main.web-application-type=none",
				"--debug=false", "--spring.main.bannerMode=OFF", "--spring.cloud.bootstrap.name=refresh")) {
			then(system.getCount()).isEqualTo(4);
			ContextRefresher refresher = new LegacyContextRefresher(context, this.scope);
			refresher.refresh();
			then(system.getCount()).isEqualTo(4);
		}
	}

	@Test
	public void commandLineArgsPassedToBootstrapConfiguration() {

		TestBootstrapConfiguration.fooSightings = new ArrayList<>();

		try (ConfigurableApplicationContext context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.web-application-type=none", "--spring.config.use-legacy-processing=true",
				"--debug=false", "--spring.main.bannerMode=OFF", "--spring.cloud.bootstrap.name=refresh",
				"--test.bootstrap.foo=bar")) {
			context.getEnvironment().setActiveProfiles("refresh");
			ContextRefresher refresher = new LegacyContextRefresher(context, this.scope);
			refresher.refresh();
			then(TestBootstrapConfiguration.fooSightings).containsExactly("bar", "bar");
		}

		TestBootstrapConfiguration.fooSightings = null;
	}

	@Test
	public void legacyContextRefresherCreatedUsingBootstrapEnabled() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withPropertyValues("spring.cloud.bootstrap.enabled=true").run(context -> {
					assertThat(context).hasSingleBean(LegacyContextRefresher.class);
					assertThat(context).hasSingleBean(ContextRefresher.class);
				});
	}

	@Test
	public void legacyContextRefresherCreated() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withPropertyValues("spring.config.use-legacy-processing=true").run(context -> {
					assertThat(context).hasSingleBean(LegacyContextRefresher.class);
					assertThat(context).hasSingleBean(ContextRefresher.class);
				});
	}

	@Test
	public void configDataContextRefresherCreated() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.run(context -> {
					assertThat(context).hasSingleBean(ConfigDataContextRefresher.class);
					assertThat(context).hasSingleBean(ContextRefresher.class);
				});
	}

	private List<String> names(MutablePropertySources propertySources) {
		List<String> list = new ArrayList<>();
		for (PropertySource<?> p : propertySources) {
			list.add(p.getName());
		}
		return list;
	}

	@Configuration(proxyBeanMethods = false)
	protected static class Empty {

	}

	@Configuration(proxyBeanMethods = false)
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class PropertySourceConfiguration implements PropertySourceLocator {

		public static Map<String, Object> MAP = new HashMap<>(
				Collections.<String, Object>singletonMap("bootstrap.foo", "refresh"));

		@Override
		public PropertySource<?> locate(Environment environment) {
			return new MapPropertySource("refreshTest", MAP);
		}

	}

	public static class TestLoggingSystem extends LoggingSystem {

		private static int count;

		public TestLoggingSystem(ClassLoader loader) {
		}

		public int getCount() {
			return count;
		}

		@Override
		public void beforeInitialize() {
			count++;
		}

	}

}
