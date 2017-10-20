package org.springframework.cloud.context.refresh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.util.TestPropertyValues;
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

	@Test
	public void orderNewPropertiesConsistentWithNewContext() {
		try (ConfigurableApplicationContext context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.webEnvironment=false", "--debug=false",
				"--spring.main.bannerMode=OFF")) {
			context.getEnvironment().setActiveProfiles("refresh");
			List<String> names = names(context.getEnvironment().getPropertySources());
			assertThat(names).doesNotContain(
					"applicationConfig: [classpath:/bootstrap-refresh.properties]");
			ContextRefresher refresher = new ContextRefresher(context, scope);
			refresher.refresh();
			names = names(context.getEnvironment().getPropertySources());
			assertThat(names)
					.contains("applicationConfig: [classpath:/bootstrap-refresh.properties]");
			assertThat(names).containsSequence(
					"applicationConfig: [classpath:/application.properties]",
					"applicationConfig: [classpath:/bootstrap-refresh.properties]");
		}
	}

	@Test
	public void bootstrapPropertySourceAlwaysFirst() {
		// Use spring.cloud.bootstrap.name to switch off the defaults (which would pick up
		// a bootstrapProperties immediately
		try (ConfigurableApplicationContext context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.webEnvironment=false", "--debug=false",
				"--spring.main.bannerMode=OFF", "--spring.cloud.bootstrap.name=refresh")) {
			List<String> names = names(context.getEnvironment().getPropertySources());
			assertThat(names).doesNotContain("bootstrapProperties");
			ContextRefresher refresher = new ContextRefresher(context, scope);
			TestPropertyValues.of(
					"spring.cloud.bootstrap.sources: org.springframework.cloud.context.refresh.ContextRefresherTests.PropertySourceConfiguration")
					.applyTo(context);
			refresher.refresh();
			names = names(context.getEnvironment().getPropertySources());
			assertThat(names).first().isEqualTo("bootstrapProperties");
		}
	}

	private List<String> names(MutablePropertySources propertySources) {
		List<String> list = new ArrayList<>();
		for (PropertySource<?> p : propertySources) {
			list.add(p.getName());
		}
		return list;
	}

	@Configuration
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class PropertySourceConfiguration implements PropertySourceLocator {

		public static Map<String, Object> MAP = new HashMap<>(
				Collections.<String, Object>singletonMap("bootstrap.context-refresh.foo", "refresh"));

		@Override
		public PropertySource<?> locate(Environment environment) {
			return new MapPropertySource("refreshTest", MAP);
		}

	}

}
