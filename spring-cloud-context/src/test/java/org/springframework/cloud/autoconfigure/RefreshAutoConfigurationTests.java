package org.springframework.cloud.autoconfigure;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.context.refresh.ContextRefresher;
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
				WebApplicationType.NONE, Config.class)) {
			assertThat(context.containsBean("refreshScope")).isTrue();
			assertThat(output.toString()).doesNotContain("WARN");
		}
	}

	@Test
	public void disabled() {
		try (ConfigurableApplicationContext context = getApplicationContext(
				WebApplicationType.SERVLET, Config.class,
				"spring.cloud.refresh.enabled:false")) {
			assertThat(context.containsBean("refreshScope")).isFalse();
		}
	}

	@Test
	public void refreshables() {
		try (ConfigurableApplicationContext context = getApplicationContext(
				WebApplicationType.NONE, Config.class, "config.foo=bar",
				"spring.cloud.refresh.refreshable:" + ConfigProps.class.getName())) {
			context.getBean(ContextRefresher.class).refresh();
		}
	}

	@Test
	public void extraRefreshables() {
		try (ConfigurableApplicationContext context = getApplicationContext(
				WebApplicationType.NONE, Config.class, "config.foo=bar",
				"spring.cloud.refresh.extra-refreshable:"
						+ ConfigProps.class.getName())) {
			context.getBean(ContextRefresher.class).refresh();
		}
	}

	private static ConfigurableApplicationContext getApplicationContext(
			WebApplicationType type, Class<?> configuration, String... properties) {
		return new SpringApplicationBuilder(configuration).web(type)
				.properties(properties).properties("server.port=0").run();
	}

	@Configuration
	@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
	@EnableConfigurationProperties
	static class Config {

	}

	@ConfigurationProperties("config")
	static class ConfigProps {
		private String foo;
		private boolean sealed;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			if (this.sealed) {
				throw new IllegalStateException("Cannot set sealed property");
			}
			this.foo = foo;
			this.sealed = true;
		}
	}
}
