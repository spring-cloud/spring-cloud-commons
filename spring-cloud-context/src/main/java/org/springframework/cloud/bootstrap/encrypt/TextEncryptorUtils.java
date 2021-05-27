/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.TextEncryptorBindHandler;
import org.springframework.cloud.bootstrap.TextEncryptorConfigBootstrapper;
import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.cloud.util.PropertyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public abstract class TextEncryptorUtils {

	/**
	 * Decrypt environment. See {@link DecryptEnvironmentPostProcessor}.
	 * @param decryptor the {@link AbstractEnvironmentDecrypt}
	 * @param environment the environment to get key properties from.
	 * @param propertySources the property sources to decrypt.
	 * @return the decrypted properties.
	 */
	static Map<String, Object> decrypt(AbstractEnvironmentDecrypt decryptor, ConfigurableEnvironment environment,
			MutablePropertySources propertySources) {
		TextEncryptor encryptor = getTextEncryptor(decryptor, environment);
		return decryptor.decrypt(encryptor, propertySources);
	}

	static TextEncryptor getTextEncryptor(AbstractEnvironmentDecrypt decryptor, ConfigurableEnvironment environment) {
		Binder binder = Binder.get(environment);
		KeyProperties keyProperties = binder.bind(KeyProperties.PREFIX, KeyProperties.class)
				.orElseGet(KeyProperties::new);
		if (TextEncryptorUtils.keysConfigured(keyProperties)) {
			decryptor.setFailOnError(keyProperties.isFailOnError());
			if (ClassUtils.isPresent("org.springframework.security.rsa.crypto.RsaSecretEncryptor", null)) {
				RsaProperties rsaProperties = binder.bind(RsaProperties.PREFIX, RsaProperties.class)
						.orElseGet(RsaProperties::new);
				return TextEncryptorUtils.createTextEncryptor(keyProperties, rsaProperties);
			}
			return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
		}
		// no keys configured
		return new TextEncryptorUtils.FailsafeTextEncryptor();
	}

	/**
	 * Register all classes that need a {@link TextEncryptor} in
	 * {@link TextEncryptorConfigBootstrapper}.
	 * @param registry the BootstrapRegistry.
	 */
	public static void register(BootstrapRegistry registry) {
		registry.registerIfAbsent(TextEncryptor.class, context -> {
			KeyProperties keyProperties = context.get(KeyProperties.class);
			if (TextEncryptorConfigBootstrapper.keysConfigured(keyProperties)) {
				if (TextEncryptorConfigBootstrapper.RSA_IS_PRESENT) {
					RsaProperties rsaProperties = context.get(RsaProperties.class);
					return createTextEncryptor(keyProperties, rsaProperties);
				}
				return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
			}
			// no keys configured
			return new FailsafeTextEncryptor();
		});
		registry.registerIfAbsent(BindHandler.class, context -> {
			TextEncryptor textEncryptor = context.get(TextEncryptor.class);
			if (textEncryptor != null) {
				KeyProperties keyProperties = context.get(KeyProperties.class);
				return new TextEncryptorBindHandler(textEncryptor, keyProperties);
			}
			return null;
		});
	}

	/**
	 * Promote the {@link TextEncryptor} to the {@link ApplicationContext}.
	 * @param bootstrapContext the Context.
	 * @param beanFactory the bean factory.
	 */
	public static void promote(BootstrapContext bootstrapContext, ConfigurableListableBeanFactory beanFactory) {
		TextEncryptor textEncryptor = bootstrapContext.get(TextEncryptor.class);
		if (textEncryptor != null) {
			beanFactory.registerSingleton("textEncryptor", textEncryptor);
		}
	}

	/**
	 * Utility to create a {@link TextEncryptor} via properties.
	 * @param keyProperties the Key properties.
	 * @param rsaProperties RSA properties.
	 * @return created {@link TextEncryptor}.
	 */
	public static TextEncryptor createTextEncryptor(KeyProperties keyProperties, RsaProperties rsaProperties) {
		KeyProperties.KeyStore keyStore = keyProperties.getKeyStore();
		if (keyStore.getLocation() != null) {
			if (keyStore.getLocation().exists()) {
				return new RsaSecretEncryptor(
						new KeyStoreKeyFactory(keyStore.getLocation(), keyStore.getPassword().toCharArray(),
								keyStore.getType()).getKeyPair(keyStore.getAlias(), keyStore.getSecret().toCharArray()),
						rsaProperties.getAlgorithm(), rsaProperties.getSalt(), rsaProperties.isStrong());
			}

			throw new IllegalStateException("Invalid keystore location");
		}

		return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
	}

	/**
	 * Is a key configured.
	 * @param properties the Key properties.
	 * @return true if configured.
	 */
	public static boolean keysConfigured(KeyProperties properties) {
		if (hasProperty(properties.getKeyStore().getLocation())) {
			if (hasProperty(properties.getKeyStore().getPassword())) {
				return true;
			}
			return false;
		}
		else if (hasProperty(properties.getKey())) {
			return true;
		}
		return false;
	}

	static boolean hasProperty(Object value) {
		if (value instanceof String) {
			return StringUtils.hasText((String) value);
		}
		return value != null;
	}

	/**
	 * Method to check if legacy bootstrap mode is enabled. This is either if the boot
	 * legacy processing property is set or spring.cloud.bootstrap.enabled=true.
	 * @param environment where to check properties.
	 * @return true if bootstrap enabled.
	 */
	public static boolean isLegacyBootstrap(Environment environment) {
		boolean isLegacy = PropertyUtils.useLegacyProcessing(environment);
		boolean isBootstrapEnabled = PropertyUtils.bootstrapEnabled(environment);
		return isLegacy || isBootstrapEnabled;
	}

	/**
	 * TextEncryptor that just fails, so that users don't get a false sense of security
	 * adding ciphers to config files and not getting them decrypted.
	 *
	 * @author Dave Syer
	 *
	 */
	public static class FailsafeTextEncryptor implements TextEncryptor {

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

}
