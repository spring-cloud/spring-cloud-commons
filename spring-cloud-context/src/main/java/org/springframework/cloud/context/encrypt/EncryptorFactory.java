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

package org.springframework.cloud.context.encrypt;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.RsaSecretEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 */
public class EncryptorFactory {

	private String salt = "deadbeef";

	public EncryptorFactory() {
	}

	public EncryptorFactory(String salt) {
		this.salt = salt;
	}

	public TextEncryptor create(String data) {

		TextEncryptor encryptor;
		if (data.contains("RSA PRIVATE KEY")) {
			encryptor = new RsaSecretEncryptor(data.replaceAll("\\n *", ""));
		}
		else if (data.startsWith("ssh-rsa") || data.contains("RSA PUBLIC KEY")) {
			throw new KeyFormatException();
		}
		else {
			encryptor = Encryptors.text(data, this.salt);
		}

		return encryptor;
	}

}
