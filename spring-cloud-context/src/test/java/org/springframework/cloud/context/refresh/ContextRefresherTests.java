package org.springframework.cloud.context.refresh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.cloud.bootstrap.TestBootstrapConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextRefresherTests {

	private RefreshScope scope = Mockito.mock(RefreshScope.class);

	@After
	public void close() {
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
		TestLoggingSystem.count = 0;
	}

	@Test
	public void orderNewPropertiesConsistentWithNewContext() {
		try (ConfigurableApplicationContext context = SpringApplication.run(Empty.class,
				"--spring.main.web-application-type=none", "--debug=false",
				"--spring.main.bannerMode=OFF")) {
			context.getEnvironment().setActiveProfiles("refresh");
			List<String> names = names(context.getEnvironment().getPropertySources());
			assertThat(names).doesNotContain(
					"applicationConfig: [classpath:/bootstrap-refresh.properties]");
			ContextRefresher refresher = new ContextRefresher(context, scope);
			refresher.refresh();
			names = names(context.getEnvironment().getPropertySources());
			assertThat(names).contains(
					"applicationConfig: [classpath:/bootstrap-refresh.properties]");
			assertThat(names).containsSequence(
					"applicationConfig: [classpath:/application.properties]",
					"applicationConfig: [classpath:/bootstrap-refresh.properties]",
					"applicationConfig: [classpath:/bootstrap.properties]");
		}
	}

	@Test
	public void bootstrapPropertySourceAlwaysFirst() {
		// Use spring.cloud.bootstrap.name to switch off the defaults (which would pick up
		// a bootstrapProperties immediately
		try (ConfigurableApplicationContext context = SpringApplication.run(Empty.class,
				"--spring.main.web-application-type=none", "--debug=false",
				"--spring.main.bannerMode=OFF",
				"--spring.cloud.bootstrap.name=refresh")) {
			List<String> names = names(context.getEnvironment().getPropertySources());
			System.err.println("***** " + context.getEnvironment().getPropertySources());
			assertThat(names).doesNotContain("bootstrapProperties");
			ContextRefresher refresher = new ContextRefresher(context, scope);
			TestPropertyValues.of(
					"spring.cloud.bootstrap.sources: org.springframework.cloud.context.refresh.ContextRefresherTests.PropertySourceConfiguration")
					.applyTo(context.getEnvironment(), Type.MAP, "defaultProperties");
			refresher.refresh();
			names = names(context.getEnvironment().getPropertySources());
			assertThat(names).first().isEqualTo("bootstrapProperties");
		}
	}

	@Test
	public void parentContextIsClosed() {
		// Use spring.cloud.bootstrap.name to switch off the defaults (which would pick up
		// a bootstrapProperties immediately
		try (ConfigurableApplicationContext context = SpringApplication.run(
				ContextRefresherTests.class, "--spring.main.web-application-type=none",
				"--debug=false", "--spring.main.bannerMode=OFF",
				"--spring.cloud.bootstrap.name=refresh")) {
			ContextRefresher refresher = new ContextRefresher(context, scope);
			TestPropertyValues.of(
					"spring.cloud.bootstrap.sources: org.springframework.cloud.context.refresh.ContextRefresherTests.PropertySourceConfiguration")
					.applyTo(context);
			ConfigurableApplicationContext refresherContext = refresher
					.addConfigFilesToEnvironment();
			assertThat(refresherContext.getParent()).isNotNull()
					.isInstanceOf(ConfigurableApplicationContext.class);
			ConfigurableApplicationContext parent = (ConfigurableApplicationContext) refresherContext
					.getParent();
			assertThat(parent.isActive()).isFalse();
		}
	}

	@Test
	public void loggingSystemNotInitialized() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY,
				TestLoggingSystem.class.getName());
		TestLoggingSystem system = (TestLoggingSystem) LoggingSystem
				.get(getClass().getClassLoader());
		assertThat(system.getCount()).isEqualTo(0);
		try (ConfigurableApplicationContext context = SpringApplication.run(Empty.class,
				"--spring.main.web-application-type=none", "--debug=false",
				"--spring.main.bannerMode=OFF",
				"--spring.cloud.bootstrap.name=refresh")) {
			assertThat(system.getCount()).isEqualTo(4);
			ContextRefresher refresher = new ContextRefresher(context, scope);
			refresher.refresh();
			assertThat(system.getCount()).isEqualTo(4);
		}
	}

	@Test
	public void commandLineArgsPassedToBootstrapConfiguration() {

		TestBootstrapConfiguration.fooSightings = new ArrayList<>();

		try (ConfigurableApplicationContext context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.web-application-type=none", "--debug=false",
				"--spring.main.bannerMode=OFF",
				"--spring.cloud.bootstrap.name=refresh",
				"--test.bootstrap.foo=bar")) {
			context.getEnvironment().setActiveProfiles("refresh");
			ContextRefresher refresher = new ContextRefresher(context, scope);
			refresher.refresh();
			assertThat(TestBootstrapConfiguration.fooSightings).containsExactly("bar", "bar");
		}

		TestBootstrapConfiguration.fooSightings = null;
	}


	private List<String> names(MutablePropertySources propertySources) {
		List<String> list = new ArrayList<>();
		for (PropertySource<?> p : propertySources) {
			list.add(p.getName());
		}
		return list;
	}

	@Configuration
	protected static class Empty {
	}

	@Configuration
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
