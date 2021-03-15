/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.bootstrap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * BindHandler that uses a TextEncryptor to decrypt text if properly prefixed with
 * {cipher}.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class TextEncryptorBindHandler extends AbstractBindHandler {

	private static final Log logger = LogFactory.getLog(TextEncryptorBindHandler.class);

	/**
	 * Prefix indicating an encrypted value.
	 */
	protected static final String ENCRYPTED_PROPERTY_PREFIX = "{cipher}";

	private final TextEncryptor textEncryptor;

	private final KeyProperties keyProperties;

	public TextEncryptorBindHandler(TextEncryptor textEncryptor, KeyProperties keyProperties) {
		this.textEncryptor = textEncryptor;
		this.keyProperties = keyProperties;
	}

	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		if (result instanceof String && ((String) result).startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
			return decrypt(name.toString(), (String) result);
		}
		return result;
	}

	private String decrypt(String key, String original) {
		String value = original.substring(ENCRYPTED_PROPERTY_PREFIX.length());
		try {
			value = this.textEncryptor.decrypt(value);
			if (logger.isDebugEnabled()) {
				logger.debug("Decrypted: key=" + key);
			}
			return value;
		}
		catch (Exception e) {
			String message = "Cannot decrypt: key=" + key;
			if (logger.isDebugEnabled()) {
				logger.warn(message, e);
			}
			else {
				logger.warn(message);
			}
			if (this.keyProperties.isFailOnError()) {
				throw new IllegalStateException(message, e);
			}
			return "";
		}
	}

}
