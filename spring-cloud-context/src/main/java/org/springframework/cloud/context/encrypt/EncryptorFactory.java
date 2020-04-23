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

package org.springframework.cloud.context.encrypt;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 */
public class EncryptorFactory {

	private static final Pattern NEWLINE_ESCAPE_PATTERN = Pattern.compile("\\r|\\n");

	private String salt = "deadbeef";

	public EncryptorFactory() {
	}

	public EncryptorFactory(String salt) {
		this.salt = salt;
	}

	public TextEncryptor create(String data) {

		TextEncryptor encryptor;
		if (data.contains("RSA PRIVATE KEY")) {

			try {
				String normalizedPemData = normalizePem(data);
				encryptor = new RsaSecretEncryptor(
						NEWLINE_ESCAPE_PATTERN.matcher(normalizedPemData).replaceAll(""));
			}
			catch (IllegalArgumentException e) {
				throw new KeyFormatException(e);
			}

		}
		else if (data.startsWith("ssh-rsa") || data.contains("RSA PUBLIC KEY")) {
			throw new KeyFormatException();
		}
		else {
			encryptor = Encryptors.text(data, this.salt);
		}

		return encryptor;
	}

	private String normalizePem(String data) {
		PEMKeyPair pemKeyPair = null;
		try (PEMParser pemParser = new PEMParser(new StringReader(data))) {
			pemKeyPair = (PEMKeyPair) pemParser.readObject();
			PrivateKeyInfo privateKeyInfo = pemKeyPair.getPrivateKeyInfo();

			StringWriter textWriter = new StringWriter();
			try (PemWriter pemWriter = new PemWriter(textWriter)) {
				PemObjectGenerator pemObjectGenerator = new MiscPEMGenerator(
						privateKeyInfo);

				pemWriter.writeObject(pemObjectGenerator);
				pemWriter.flush();
				return textWriter.toString();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
