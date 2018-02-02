/*
 * Copyright 2018 the original author or authors.
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

import org.junit.Test;
import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author Biju Kunjummen
 */
public class EncryptorFactoryTests {

	@Test
	public void testWithRsaPrivateKey() throws Exception {
		String key = StreamUtils.copyToString(
				new ClassPathResource("/example-test-rsa-private-key").getInputStream(),
				Charset.forName("ASCII"));
		//RSA private key needs to be with no new lines 
		//-----BEGIN RSA PRIVATE KEY-----MIIEowI....iX8htsO-----END RSA PRIVATE KEY-----
		String keyNoNewLines = key.replaceAll("\\n", "");
		TextEncryptor encryptor = new EncryptorFactory().create(keyNoNewLines);
		String toEncrypt = "sample text to encrypt";
		String encrypted = encryptor.encrypt(toEncrypt);

		assertEquals(toEncrypt, encryptor.decrypt(encrypted));
	}
}
