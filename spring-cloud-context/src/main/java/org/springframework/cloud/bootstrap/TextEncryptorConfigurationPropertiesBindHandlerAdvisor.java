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

package org.springframework.cloud.bootstrap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class TextEncryptorConfigurationPropertiesBindHandlerAdvisor implements ConfigurationPropertiesBindHandlerAdvisor {

	private final ApplicationContext applicationContext;

	public TextEncryptorConfigurationPropertiesBindHandlerAdvisor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public BindHandler apply(BindHandler bindHandler) {
		return wrappedBindHandler(bindHandler);
	}

	private BindHandler wrappedBindHandler(BindHandler handler) {
		KeyProperties keyProperties = this.applicationContext.getBean(KeyProperties.class);
		TextEncryptorBindHandler textEncryptorBindHandler = new TextEncryptorBindHandler(this.applicationContext.getBeanProvider(TextEncryptor.class).getIfAvailable(FailsafeTextEncryptor::new), keyProperties);
		return new BindHandler() {
			@Override
			public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
				return handler.onStart(name, target, context);
			}

			@Override
			public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
				Object defaultHandlerResult = handler.onSuccess(name, target, context, result);
				return textEncryptorBindHandler.onSuccess(name, target, context, defaultHandlerResult);
			}

			@Override
			public Object onCreate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
				return handler.onCreate(name, target, context, result);
			}

			@Override
			public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
				return handler.onFailure(name, target, context, error);
			}

			@Override
			public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
				handler.onFinish(name, target, context, result);
			}
		};
	}

	/**
	 * TextEncryptor that just fails, so that users don't get a false sense of security
	 * adding ciphers to config files and not getting them decrypted.
	 *
	 * @author Dave Syer
	 *
	 */
	protected static class FailsafeTextEncryptor implements TextEncryptor {

		@Override
		public String encrypt(String text) {
			throw new UnsupportedOperationException(
					"No encryption for FailsafeTextEncryptor. Did you configure the keystore correctly?");
		}

		@Override
		public String decrypt(String encryptedText) {
			throw new UnsupportedOperationException(
					"No decryption for FailsafeTextEncryptor. Did you configure the keystore correctly?");
		}

	}

	protected static class TextEncryptorBindHandler extends AbstractBindHandler {

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
			if (result instanceof String) {
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
}
