package org.springframework.cloud.bootstrap.encrypt;

/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.RsaAlgorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EncryptionBootstrapConfigurationTests {

	@Test
	public void symmetric() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				EncryptionBootstrapConfiguration.class).web(WebApplicationType.NONE)
						.properties("encrypt.key:pie").run();
		TextEncryptor encryptor = context.getBean(TextEncryptor.class);
		assertEquals("foo", encryptor.decrypt(encryptor.encrypt("foo")));
		context.close();
	}

	@Test
	public void rsaKeyStore() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				EncryptionBootstrapConfiguration.class)
						.web(WebApplicationType.NONE)
						.properties("encrypt.keyStore.location:classpath:/server.jks",
								"encrypt.keyStore.password:letmein",
								"encrypt.keyStore.alias:mytestkey",
								"encrypt.keyStore.secret:changeme")
						.run();
		TextEncryptor encryptor = context.getBean(TextEncryptor.class);
		assertEquals("foo", encryptor.decrypt(encryptor.encrypt("foo")));
		context.close();
	}

	@Test
	public void rsaKeyStoreWithRelaxedProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				EncryptionBootstrapConfiguration.class)
						.web(WebApplicationType.NONE)
						.properties("encrypt.key-store.location:classpath:/server.jks",
								"encrypt.key-store.password:letmein",
								"encrypt.key-store.alias:mytestkey",
								"encrypt.key-store.secret:changeme")
						.run();
		TextEncryptor encryptor = context.getBean(TextEncryptor.class);
		assertEquals("foo", encryptor.decrypt(encryptor.encrypt("foo")));
		context.close();
	}


	@Test
	public void rsaProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				EncryptionBootstrapConfiguration.class).web(WebApplicationType.NONE).properties(
				"encrypt.key-store.location:classpath:/server.jks",
				"encrypt.key-store.password:letmein",
				"encrypt.key-store.alias:mytestkey", "encrypt.key-store.secret:changeme",
				"encrypt.rsa.strong:true",
				"encrypt.rsa.salt:foobar")
				.run();
		RsaProperties properties = context.getBean(RsaProperties.class);
		assertEquals("foobar", properties.getSalt());
		assertTrue(properties.isStrong());
		assertEquals(RsaAlgorithm.DEFAULT, properties.getAlgorithm());
		context.close();
	}


	@Test
	public void nonExistentKeystoreLocationShouldNotBeAllowed() {
		try {
			new SpringApplicationBuilder(EncryptionBootstrapConfiguration.class)
					.web(WebApplicationType.NONE)
					.properties("encrypt.key-store.location:classpath:/server.jks1",
							"encrypt.key-store.password:letmein",
							"encrypt.key-store.alias:mytestkey",
							"encrypt.key-store.secret:changeme")
					.run();
			assertThat(false).as(
					"Should not create an application context with invalid keystore location")
					.isTrue();
		}
		catch (Exception e) {
			assertThat(e).hasRootCauseInstanceOf(IllegalStateException.class);
		}
	}
}
