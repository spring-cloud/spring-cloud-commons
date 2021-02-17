/*
 * Copyright 2017-2020 the original author or authors.
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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContextBuilder;

import org.springframework.core.io.Resource;

public class SSLContextFactory {

	private TlsProperties properties;

	public SSLContextFactory(TlsProperties properties) {
		this.properties = properties;
	}

	public SSLContext createSSLContext() throws GeneralSecurityException, IOException {
		SSLContextBuilder builder = new SSLContextBuilder();
		char[] keyPassword = properties.keyPassword();
		KeyStore keyStore = createKeyStore();

		try {
			builder.loadKeyMaterial(keyStore, keyPassword);
		}
		catch (UnrecoverableKeyException e) {
			if (keyPassword.length == 0) {
				// Retry if empty password, see
				// https://rt.openssl.org/Ticket/Display.html?id=1497&user=guest&pass=guest
				builder.loadKeyMaterial(keyStore, new char[] { '\0' });
			}
			else {
				throw e;
			}
		}

		KeyStore trust = createTrustStore();
		if (trust != null) {
			builder.loadTrustMaterial(trust, null);
		}

		return builder.build();
	}

	public KeyStore createKeyStore() throws GeneralSecurityException, IOException {
		if (properties.getKeyStore() == null) {
			throw new KeyStoreException("Keystore not specified.");
		}
		if (!properties.getKeyStore().exists()) {
			throw new KeyStoreException("Keystore not exists: " + properties.getKeyStore());
		}

		KeyStore result = KeyStore.getInstance(properties.getKeyStoreType());
		char[] keyStorePassword = properties.keyStorePassword();

		try {
			loadKeyStore(result, properties.getKeyStore(), keyStorePassword);
		}
		catch (IOException e) {
			// Retry if empty password, see
			// https://rt.openssl.org/Ticket/Display.html?id=1497&user=guest&pass=guest
			if (keyStorePassword.length == 0) {
				loadKeyStore(result, properties.getKeyStore(), new char[] { '\0' });
			}
			else {
				throw e;
			}
		}

		return result;
	}

	private static void loadKeyStore(KeyStore keyStore, Resource keyStoreResource, char[] keyStorePassword)
			throws IOException, GeneralSecurityException {
		try (InputStream inputStream = keyStoreResource.getInputStream()) {
			keyStore.load(inputStream, keyStorePassword);
		}
	}

	public KeyStore createTrustStore() throws GeneralSecurityException, IOException {
		if (properties.getTrustStore() == null) {
			return null;
		}
		if (!properties.getTrustStore().exists()) {
			throw new KeyStoreException("KeyStore not exists: " + properties.getTrustStore());
		}

		KeyStore result = KeyStore.getInstance(properties.getTrustStoreType());
		try (InputStream input = properties.getTrustStore().getInputStream()) {
			result.load(input, properties.trustStorePassword());
		}
		return result;
	}

}
