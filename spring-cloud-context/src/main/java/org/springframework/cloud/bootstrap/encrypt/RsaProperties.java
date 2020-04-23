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

package org.springframework.cloud.bootstrap.encrypt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.rsa.crypto.RsaAlgorithm;

/**
 * @author Ryan Baxter
 */
@ConditionalOnClass(RsaAlgorithm.class)
@ConfigurationProperties("encrypt.rsa")
public class RsaProperties {

	/**
	 * The RSA algorithm to use (DEFAULT or OEAP). Once it is set, do not change it (or
	 * existing ciphers will not be decryptable).
	 */
	private RsaAlgorithm algorithm = RsaAlgorithm.DEFAULT;

	/**
	 * Flag to indicate that "strong" AES encryption should be used internally. If true,
	 * then the GCM algorithm is applied to the AES encrypted bytes. Default is false (in
	 * which case "standard" CBC is used instead). Once it is set, do not change it (or
	 * existing ciphers will not be decryptable).
	 */
	private boolean strong = false;

	/**
	 * Salt for the random secret used to encrypt cipher text. Once it is set, do not
	 * change it (or existing ciphers will not be decryptable).
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
