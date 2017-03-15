package org.springframework.cloud.bootstrap;

import java.util.Collections;

import org.springframework.context.ApplicationContextInitializer;
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
public class TestBootstrapConfiguration {

	public TestBootstrapConfiguration() {
		firstToBeCreated.compareAndSet(null, TestBootstrapConfiguration.class);
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
