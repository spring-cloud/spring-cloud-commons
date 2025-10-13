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

package org.springframework.cloud.bootstrap.encrypt;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 */
public class EncryptorFactoryTests {

	@Test
	public void testWithRsaPrivateKey() throws Exception {
		String key = StreamUtils.copyToString(new ClassPathResource("/example-test-rsa-private-key").getInputStream(),
				StandardCharsets.US_ASCII);

		TextEncryptor encryptor = new EncryptorFactory().create(key);
		String toEncrypt = "sample text to encrypt";
		String encrypted = encryptor.encrypt(toEncrypt);

		then(encryptor.decrypt(encrypted)).isEqualTo(toEncrypt);
	}

	@Test
	public void testWithInvalidRsaPrivateKey() {
		String key = """
				-----BEGIN RSA PRIVATE KEY-----
				MIIEowIBAAKCAQEAwClFgrRa/PUHPIJr9gvIPL6g6Rjp/TVZmVNOf2fL96DYbkj5
				""";
		assertThatRuntimeException().isThrownBy(() -> new EncryptorFactory().create(key));
	}

}
