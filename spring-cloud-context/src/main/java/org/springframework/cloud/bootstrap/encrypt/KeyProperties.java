/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.cloud.bootstrap.encrypt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.security.rsa.crypto.RsaAlgorithm;
import org.springframework.util.ClassUtils;

@ConfigurationProperties("encrypt")
public class KeyProperties {

	/**
	 * A symmetric key. As a stronger alternative consider using a keystore.
	 */
	private String key;

	/**
	 * Flag to say that a process should fail if there is an encryption or decryption
	 * error.
	 */
	private boolean failOnError = true;

	/**
	 * The key store properties for locating a key in a Java Key Store (a file in a format
	 * defined and understood by the JVM).
	 */
	private KeyStore keyStore = new KeyStore();

	/**
	 * Rsa algorithm properties when using asymmetric encryption.
	 */
	private Rsa rsa;

	{
		if (ClassUtils.isPresent("org.springframework.security.rsa.crypto.RsaAlgorithm",
				null)) {
			this.rsa = new Rsa();
		}
	}

	public Rsa getRsa() {
		return this.rsa;
	}

	public boolean isFailOnError() {
		return this.failOnError;
	}

	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public KeyStore getKeyStore() {
		return this.keyStore;
	}

	public void setKeyStore(KeyProperties.KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	public static class KeyStore {

		/**
		 * Location of the key store file, e.g. classpath:/keystore.jks.
		 */
		private Resource location;

		/**
		 * Password that locks the keystore.
		 */
		private String password;

		/**
		 * Alias for a key in the store.
		 */
		private String alias;

		/**
		 * Secret protecting the key (defaults to the same as the password).
		 */
		private String secret;

		public String getAlias() {
			return this.alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public Resource getLocation() {
			return this.location;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getSecret() {
			return this.secret == null ? this.password : this.secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

	}

	public static class Rsa {

		/**
		 * The RSA algorithm to use (DEFAULT or OAEP). Once it is set do not change it (or
		 * existing ciphers will not a decryptable).
		 */
		private RsaAlgorithm algorithm = RsaAlgorithm.OAEP;

		/**
		 * Flag to indicate that "strong" AES encryption should be used internally. If
		 * true then the GCM algorithm is applied to the AES encrypted bytes. If false
		 * then the "standard" CBC is used instead. Once it is set do not change it (or
		 * existing ciphers will not a decryptable).
		 */
		private boolean strong = true;

		/**
		 * Salt for the random secret used to encrypt cipher text. Once it is set do not
		 * change it (or existing ciphers will not a decryptable).
		 */
		private String salt = "deadbeef";

		public RsaAlgorithm getAlgorithm() {
			return this.algorithm;
		}

		public void setAlgorithm(RsaAlgorithm algorithm) {
			this.algorithm = algorithm;
		}

		public boolean isStrong() {
			return this.strong;
		}

		public void setStrong(boolean strong) {
			this.strong = strong;
		}

		public String getSalt() {
			return this.salt;
		}

		public void setSalt(String salt) {
			this.salt = salt;
		}

	}
}