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

package org.springframework.cloud.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class SSHContextFactoryTests {

	private static final String KEY_STORE_PASSWORD = "test-key-store-password";

	private static final String KEY_PASSWORD = "test-key-password";

	private static KeyAndCert ca;

	private static KeyAndCert cert;

	private static File keyStore;

	private static File trustStore;

	private TlsProperties properties;

	@BeforeClass
	public static void createKeyStoreAndTrustStore() throws Exception {
		KeyTool tool = new KeyTool();

		ca = tool.createCA("MyCA");
		cert = ca.sign("MyCert");

		keyStore = saveKeyAndCert(cert);
		trustStore = saveCert(ca);
	}

	private static File saveKeyAndCert(KeyAndCert keyCert) throws Exception {
		return saveKeyStore(keyCert.subject(),
				() -> keyCert.storeKeyAndCert(KEY_PASSWORD));
	}

	private static File saveCert(KeyAndCert keyCert) throws Exception {
		return saveKeyStore(keyCert.subject(), () -> keyCert.storeCert());
	}

	private static File saveKeyStore(String prefix, KeyStoreSupplier func)
			throws Exception {
		File result = File.createTempFile(prefix, ".p12");
		result.deleteOnExit();

		try (OutputStream output = new FileOutputStream(result)) {
			KeyStore store = func.createKeyStore();
			store.store(output, KEY_STORE_PASSWORD.toCharArray());
		}
		return result;
	}

	@Before
	public void createProperties() {
		properties = new TlsProperties();

		properties.setEnabled(true);
		properties.setKeyStore(resourceOf(keyStore));
		properties.setKeyStorePassword(KEY_STORE_PASSWORD);
		properties.setKeyPassword(KEY_PASSWORD);
		properties.setTrustStore(resourceOf(trustStore));
		properties.setTrustStorePassword(KEY_STORE_PASSWORD);

		properties.postConstruct();
	}

	private Resource resourceOf(File file) {
		return new FileSystemResource(file);
	}

	@Test
	public void createKeyStoreFromProperties()
			throws GeneralSecurityException, IOException {
		SSLContextFactory factory = new SSLContextFactory(properties);
		KeyStore store = factory.createKeyStore();

		Certificate c = store.getCertificate("MyCert");
		assertThat(c).isEqualTo(cert.certificate());

		Key key = store.getKey("MyCert", KEY_PASSWORD.toCharArray());
		assertThat(key).isEqualTo(cert.privateKey());
	}

	@Test
	public void createTrustStoreFromProperties()
			throws GeneralSecurityException, IOException {
		SSLContextFactory factory = new SSLContextFactory(properties);
		KeyStore store = factory.createTrustStore();

		Certificate c = store.getCertificate("MyCA");
		assertThat(c).isEqualTo(ca.certificate());
	}

	@Test
	public void createSSLContextFromProperties()
			throws GeneralSecurityException, IOException {
		SSLContextFactory factory = new SSLContextFactory(properties);
		SSLContext context = factory.createSSLContext();
		assertThat(context).isNotNull();
	}

	interface KeyStoreSupplier {

		KeyStore createKeyStore() throws Exception;

	}

}
