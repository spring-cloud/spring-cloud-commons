/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.bootstrap.encrypt;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.test.TestConfigDataLocationResolver;
import org.springframework.cloud.context.test.TestEnvPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

public class EncryptionIntegrationTests {

	@Test
	public void legacySymmetricPropertyValues() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
				.web(WebApplicationType.NONE).properties("spring.config.use-legacy-processing=true", "encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getEnvironment().getProperty("foo.password")).isEqualTo("test");
	}

	@Test
	public void legacySymmetricConfigurationProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
				.web(WebApplicationType.NONE).properties("spring.config.use-legacy-processing=true", "encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getBean(PasswordProperties.class).getPassword()).isEqualTo("test");
	}

	@Test
	public void propSymmetricPropertyValues() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
				.web(WebApplicationType.NONE).properties("spring.cloud.bootstrap.enabled=true", "encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getEnvironment().getProperty("foo.password")).isEqualTo("test");
	}

	@Test
	public void propSymmetricConfigurationProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
				.web(WebApplicationType.NONE).properties("spring.cloud.bootstrap.enabled=true", "encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getBean(PasswordProperties.class).getPassword()).isEqualTo("test");
	}

	@Test
	public void symmetricPropertyValuesFailOnError() {
		assertThatThrownBy(() -> {
			ConfigurableApplicationContext context = new SpringApplicationBuilder(TestAutoConfiguration.class)
					.web(WebApplicationType.NONE)
					.properties("spring.config.use-legacy-processing=false", "encrypt.key:pie",
							"foo.password:{cipher}ZZZbf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
					.run();
		}).isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot decrypt");
	}

	@Test
	public void symmetricPropertyValuesFailOnErrorFalse() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestAutoConfiguration.class)
				.web(WebApplicationType.NONE)
				.properties("encrypt.fail-on-error=false", "spring.config.use-legacy-processing=false",
						"encrypt.key:pie",
						"foo.password:{cipher}ZZZbf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getEnvironment().getProperty("foo.password")).isEmpty();
	}

	@Test
	public void symmetricPropertyValues() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestAutoConfiguration.class)
				.web(WebApplicationType.NONE).properties("spring.config.use-legacy-processing=false", "encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getEnvironment().getProperty("foo.password")).isEqualTo("test");
	}

	@Test
	public void decryptEnvironmentPostProcessorDisabled() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestAutoConfiguration.class)
				.web(WebApplicationType.NONE)
				.properties("spring.config.use-legacy-processing=false", "encrypt.key:pie",
						"spring.cloud.decrypt-environment-post-processor.enabled=false",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getEnvironment().getProperty("foo.password")).startsWith("{cipher}bf2945");
	}

	@Test
	public void symmetricConfigurationProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestAutoConfiguration.class)
				.web(WebApplicationType.NONE).properties("spring.config.use-legacy-processing=false", "encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
				.run();
		then(context.getBean(PasswordProperties.class).getPassword()).isEqualTo("test");
	}

	@Test
	public void decryptAfterRefresh() {
		TestConfigDataLocationResolver.config.put("foo.password",
				"{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95");
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestAutoConfiguration.class)
				.web(WebApplicationType.NONE).properties("encrypt.key:pie", TestEnvPostProcessor.EPP_ENABLED + "=true",
						"spring.cloud.refresh.enabled:true")
				.run();
		TextEncryptor encryptor = context.getBean(TextEncryptor.class);
		ContextRefresher refresher = context.getBean(ContextRefresher.class);
		ConfigurableEnvironment env = context.getBean(ConfigurableEnvironment.class);
		then(env.getProperty("foo.password")).isEqualTo("test");
		TestConfigDataLocationResolver.config.put("foo.password", "{cipher}" + encryptor.encrypt("newValue"));
		refresher.refresh();
		then(env.getProperty("foo.password")).isEqualTo("newValue");
		context.close();
		TestConfigDataLocationResolver.config.clear();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PasswordProperties.class)
	protected static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableConfigurationProperties(PasswordProperties.class)
	protected static class TestAutoConfiguration {

	}

	@ConfigurationProperties("foo")
	protected static class PasswordProperties {

		private String password;

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

}
