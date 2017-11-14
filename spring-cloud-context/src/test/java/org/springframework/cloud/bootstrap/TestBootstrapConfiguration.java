package org.springframework.cloud.bootstrap;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import static org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration.firstToBeCreated;

/**
 * @author Spencer Gibb
 */
@Order(0)
@Configuration
@EnableConfigurationProperties
public class TestBootstrapConfiguration {

	public TestBootstrapConfiguration() {
		firstToBeCreated.compareAndSet(null, TestBootstrapConfiguration.class);
	}

	public static List<String> fooSightings = null;

	@Bean
	@Qualifier("foo-during-bootstrap")
	public String fooDuringBootstrap(ConfigurableEnvironment environment, ApplicationEventPublisher publisher) {
		String property = environment.getProperty("test.bootstrap.foo", "undefined");

		if (fooSightings != null) {
			fooSightings.add(property);
		}

		return property;
	}

	@Bean
	public ApplicationContextInitializer<ConfigurableApplicationContext> customInitializer() {
		return new ApplicationContextInitializer<ConfigurableApplicationContext>() {

			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {
				ConfigurableEnvironment environment = applicationContext.getEnvironment();
				environment.getPropertySources()
						.addLast(new MapPropertySource("customProperties",
								Collections.<String, Object>singletonMap("custom.foo",
										environment.resolvePlaceholders(
												"${spring.application.name:bar}"))));
			}

		};
	}

}
