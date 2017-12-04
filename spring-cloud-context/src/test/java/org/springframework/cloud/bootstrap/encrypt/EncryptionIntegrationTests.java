package org.springframework.cloud.bootstrap.encrypt;

import org.junit.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;

public class EncryptionIntegrationTests {

	@Test
	public void symmetricPropertyValues() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).properties(
						"encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
						.run();
		assertEquals("test", context.getEnvironment().getProperty("foo.password"));
	}

	@Test
	public void symmetricConfigurationProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).properties(
						"encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
						.run();
		assertEquals("test", context.getBean(PasswordProperties.class).getPassword());
	}

	@Configuration
	@EnableConfigurationProperties(PasswordProperties.class)
	protected static class TestConfiguration {

	}

	@ConfigurationProperties("foo")
	protected static class PasswordProperties {
		private String password;

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
