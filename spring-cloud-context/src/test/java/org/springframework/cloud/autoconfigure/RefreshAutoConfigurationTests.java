package org.springframework.cloud.autoconfigure;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Dave Syer
 */
public class RefreshAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void noWarnings() {
		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class)) {
			assertThat(context.containsBean("refreshScope")).isTrue();
			assertThat(output.toString()).doesNotContain("WARN");
		}
	}

	@Test
	public void disabled() {
		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class, "spring.cloud.refresh.enabled:false")) {
			assertThat(context.containsBean("refreshScope")).isFalse();
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
