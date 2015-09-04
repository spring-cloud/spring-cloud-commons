package org.springframework.cloud.client.actuator;


import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Spencer Gibb
 */
public class FeaturesEndpointTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JacksonAutoConfiguration.class, FeaturesConfig.class, Config.class);
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
		FeaturesEndpoint.Features features = this.context.getBean(FeaturesEndpoint.class).invoke();
		assertThat(features, is(notNullValue()));
		assertThat(features.getEnabled().size(), is(equalTo(2)));
		assertThat(features.getDisabled().size(), is(equalTo(1)));
	}

	@Configuration
	public static class FeaturesConfig {
		@Bean
		Foo foo() {
			return new Foo();
		}

		@Bean
		Bar bar() {
			return new Bar();
		}

		@Bean
		HasFeatures localFeatures() {
			return HasFeatures.builder()
					.abstractFeature(Foo.class)
					.namedFeature(new NamedFeature("Bar Feature", Bar.class))
					.abstractFeature(Baz.class)
					.build();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {
		@Autowired(required = false)
		private List<HasFeatures> hasFeatures = new ArrayList<>();

		@Bean
		public FeaturesEndpoint cloudEndpoint() {
			return new FeaturesEndpoint(hasFeatures);
		}
	}

	public static class Foo {}
	public static class Bar {}
	public static class Baz {}
}
