package org.springframework.cloud.autoconfigure;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.endpoint.event.RefreshEventListener;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({"spring-boot-actuator-*.jar", "spring-boot-starter-actuator-*.jar"})
public class RefreshAutoConfigurationClassPathTests {

	@Test
	public void refreshEventListenerCreated() {
		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class)) {
			assertThat(context.getBeansOfType(RefreshEventListener.class)).as("RefreshEventListeners not created").isNotEmpty();
			assertThat(context.containsBean("refreshEndpoint")).as("refreshEndpoint created").isFalse();
		}
	}

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration, String... properties) {
		return new SpringApplicationBuilder(configuration).web(WebApplicationType.NONE).properties(properties).run();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}
}
