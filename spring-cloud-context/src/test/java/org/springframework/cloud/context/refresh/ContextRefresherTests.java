package org.springframework.cloud.context.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

public class ContextRefresherTests {

	private RefreshScope scope = Mockito.mock(RefreshScope.class);
	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void orderNewPropertiesConsistentWithNewContext() {
		context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.webEnvironment=false", "--debug=false",
				"--spring.main.bannerMode=OFF");
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
				"applicationConfig: [classpath:/bootstrap-refresh.properties]",
				"applicationConfig: [classpath:/bootstrap.properties]");
	}

	@Test
	public void bootstrapPropertySourceAlwaysFirst() {
		// Use spring.cloud.bootstrap.name to switch off the defaults (which would pick up
		// a bootstrapProperties immediately
		context = SpringApplication.run(ContextRefresherTests.class,
				"--spring.main.webEnvironment=false", "--debug=false",
				"--spring.main.bannerMode=OFF", "--spring.cloud.bootstrap.name=refresh");
		List<String> names = names(context.getEnvironment().getPropertySources());
		assertThat(names).doesNotContain("bootstrapProperties");
		ContextRefresher refresher = new ContextRefresher(context, scope);
		EnvironmentTestUtils.addEnvironment(context,
				"spring.cloud.bootstrap.sources: org.springframework.cloud.context.refresh.ContextRefresherTests.PropertySourceConfiguration\n"
						+ "");
		refresher.refresh();
		names = names(context.getEnvironment().getPropertySources());
		assertThat(names).first().isEqualTo("bootstrapProperties");
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

		public static Map<String, Object> MAP = new HashMap<String, Object>(
				Collections.<String, Object>singletonMap("bootstrap.foo", "refresh"));

		@Override
		public PropertySource<?> locate(Environment environment) {
			return new MapPropertySource("refreshTest", MAP);
		}

	}

}
