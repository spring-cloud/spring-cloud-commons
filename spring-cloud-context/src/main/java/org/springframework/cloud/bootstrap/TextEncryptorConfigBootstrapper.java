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

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.Bootstrapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.cloud.bootstrap.encrypt.RsaProperties;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.ClassUtils;

/**
 * Bootstrapper
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class TextEncryptorConfigBootstrapper implements Bootstrapper {

	@Override
	public void intitialize(BootstrapRegistry registry) {
		if (!ClassUtils.isPresent("org.springframework.security.crypto.encrypt.TextEncryptor", null)) {
			return;
		}

		registry.registerIfAbsent(KeyProperties.class,
				context -> context.get(Binder.class)
						.bind("encrypt", KeyProperties.class)
						.orElseGet(KeyProperties::new));
		registry.registerIfAbsent(RsaProperties.class,
				context -> context.get(Binder.class)
						.bind("encrypt.rsa", RsaProperties.class)
						.orElseGet(RsaProperties::new));


		/*registry.registerIfAbsent(TextEncryptorConfigurationPropertiesBindHandlerAdvisor.class, context -> {
			if (isLegacyProcessingEnabled(context.get(Binder.class))) {
				return null;
			}
			return new TextEncryptorConfigurationPropertiesBindHandlerAdvisor(context.get(TextEncryptor.class));
		});*/

		// promote beans to context
		registry.addCloseListener(event -> {
			if (isLegacyProcessingEnabled(event.getApplicationContext().getEnvironment())) {
				return;
			}
			KeyProperties keyProperties = event.getBootstrapContext().get(KeyProperties.class);
			if (keyProperties != null) {
				event.getApplicationContext().getBeanFactory().registerSingleton("keyProperties",
						keyProperties);
			}
			RsaProperties rsaProperties = event.getBootstrapContext().get(RsaProperties.class);
			if (rsaProperties != null) {
				event.getApplicationContext().getBeanFactory().registerSingleton("rsaProperties",
						rsaProperties);
			}
		});
	}

	private boolean isLegacyProcessingEnabled(Binder binder) {
		return binder.bind("spring.config.use-legacy-processing", Boolean.class).orElse(false);
	}

	private boolean isLegacyProcessingEnabled(Environment environment) {
		return environment.getProperty("spring.config.use-legacy-processing", Boolean.class, false);
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
}
