/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class SSHContextFactoryTests {

	private static final String KEY_STORE_PASSWORD = "test-key-store-password";

	private static final String KEY_PASSWORD = "test-key-password";

	private TlsProperties properties;

	@BeforeEach
	public void createProperties() {
		properties = new TlsProperties();

		properties.setEnabled(true);
		properties.setKeyStore(resourceOf("MyCert.p12"));
		properties.setKeyStorePassword(KEY_STORE_PASSWORD);
		properties.setKeyPassword(KEY_PASSWORD);
		properties.setTrustStore(resourceOf("MyCA.p12"));
		properties.setTrustStorePassword(KEY_STORE_PASSWORD);
	}

	private Resource resourceOf(String path) {
		return new ClassPathResource(path);
	}

	@Test
	public void createKeyStoreFromProperties() throws GeneralSecurityException, IOException {
		SSLContextFactory factory = new SSLContextFactory(properties);
		KeyStore store = factory.createKeyStore();

		Certificate c = store.getCertificate("MyCert");
		assertThat(c).isNotNull();

		Key key = store.getKey("MyCert", KEY_PASSWORD.toCharArray());
		assertThat(key).isNotNull();
	}

	@Test
	public void createTrustStoreFromProperties() throws GeneralSecurityException, IOException {
		SSLContextFactory factory = new SSLContextFactory(properties);
		KeyStore store = factory.createTrustStore();

		Certificate c = store.getCertificate("MyCA");
		assertThat(c).isNotNull();
	}

	@Test
	public void createSSLContextFromProperties() throws GeneralSecurityException, IOException {
		SSLContextFactory factory = new SSLContextFactory(properties);
		SSLContext context = factory.createSSLContext();
		assertThat(context).isNotNull();
	}

	interface KeyStoreSupplier {

		KeyStore createKeyStore() throws Exception;

	}

}
