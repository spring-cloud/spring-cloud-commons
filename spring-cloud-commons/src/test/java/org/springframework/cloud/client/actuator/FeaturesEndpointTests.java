package org.springframework.cloud.client.actuator;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Spencer Gibb
 */
public class FeaturesEndpointTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JacksonAutoConfiguration.class, FeaturesConfig.class,
				Config.class);
		this.context.refresh();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void invokeWorks() {
		FeaturesEndpoint.Features features = this.context.getBean(FeaturesEndpoint.class)
				.features();
		assertThat(features).isNotNull();
		assertThat(features.getEnabled()).hasSize(2)
				.contains(newFeature("foo", Foo.class), newFeature("Baz Feature", Baz.class));
		assertThat(features.getDisabled()).hasSize(1).contains("Bar");
	}

	private FeaturesEndpoint.Feature newFeature(String name, Class<?> type) {
		return new FeaturesEndpoint.Feature(name, type.getCanonicalName(), null, null);
	}

	@Configuration
	public static class FeaturesConfig {
		@Bean
		Foo foo() {
			return new Foo();
		}

		@Bean
		HasFeatures localFeatures() {
			HasFeatures features = HasFeatures.namedFeatures(
					new NamedFeature("foo", Foo.class),
					new NamedFeature("Baz Feature", Baz.class));
			features.getAbstractFeatures().add(Bar.class);
			return features;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {
		@Autowired(required = false)
		private List<HasFeatures> hasFeatures = new ArrayList<>();

		@Bean
		public FeaturesEndpoint cloudEndpoint() {
			return new FeaturesEndpoint(this.hasFeatures);
		}
	}

	public static class Foo {
	}

	public static class Bar {
	}

	public static class Baz {
	}
}
