package org.springframework.cloud.autoconfigure;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
			assertTrue(context.containsBean("refreshScope"));
			assertThat(output.toString(), not(containsString("WARN")));
		}
	}

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration, String... properties) {
		return new SpringApplicationBuilder(configuration).web(false).properties(properties).run();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}
}
