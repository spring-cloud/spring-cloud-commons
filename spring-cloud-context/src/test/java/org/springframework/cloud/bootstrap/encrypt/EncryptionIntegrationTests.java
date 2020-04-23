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

import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

public class EncryptionIntegrationTests {

	@Test
	public void symmetricPropertyValues() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).properties(
						"encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
						.run();
		then(context.getEnvironment().getProperty("foo.password")).isEqualTo("test");
	}

	@Test
	public void symmetricConfigurationProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).properties(
						"encrypt.key:pie",
						"foo.password:{cipher}bf29452295df354e6153c5b31b03ef23c70e55fba24299aa85c63438f1c43c95")
						.run();
		then(context.getBean(PasswordProperties.class).getPassword()).isEqualTo("test");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PasswordProperties.class)
	protected static class TestConfiguration {

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
