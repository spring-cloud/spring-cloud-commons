package org.springframework.cloud.bootstrap.encrypt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class EncryptionBootstrapConfigurationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void rsaKeyStore() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				EncryptionBootstrapConfiguration.class).web(false).properties(
				"encrypt.keyStore.location:classpath:/server.jks",
				"encrypt.keyStore.password:letmein",
				"encrypt.keyStore.alias:mytestkey", "encrypt.keyStore.secret:changeme")
				.run();
		TextEncryptor encryptor = context.getBean(TextEncryptor.class);
		assertEquals("foo", encryptor.decrypt(encryptor.encrypt("foo")));
		context.close();
	}

	@Test
	public void rsaKeyStoreWithRelaxedProperties() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				EncryptionBootstrapConfiguration.class).web(false).properties(
				"encrypt.key-store.location:classpath:/server.jks",
				"encrypt.key-store.password:letmein",
				"encrypt.key-store.alias:mytestkey", "encrypt.key-store.secret:changeme")
				.run();
		TextEncryptor encryptor = context.getBean(TextEncryptor.class);
		assertEquals("foo", encryptor.decrypt(encryptor.encrypt("foo")));
		context.close();
	}

	@Test
	public void nonExistentKeystoreLocationShouldNotBeAllowed() {
		try {
			new SpringApplicationBuilder(EncryptionBootstrapConfiguration.class)
					.web(false)
					.properties("encrypt.key-store.location:classpath:/server.jks1",
							"encrypt.key-store.password:letmein",
							"encrypt.key-store.alias:mytestkey",
							"encrypt.key-store.secret:changeme")
					.run();
			assertThat(false)
					.as("Should not create an application context with invalid keystore location")
					.isTrue();
		}
		catch (Exception e) {
			assertThat(e).hasRootCauseInstanceOf(IllegalStateException.class);
		}
	}

}
