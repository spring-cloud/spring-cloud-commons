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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.cloud.bootstrap.encrypt.RsaProperties;
import org.springframework.cloud.bootstrap.encrypt.TextEncryptorUtils;
import org.springframework.util.ClassUtils;

/**
 * Bootstrapper.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class TextEncryptorConfigBootstrapper implements BootstrapRegistryInitializer {

	/**
	 * RsaSecretEncryptor present.
	 */
	public static final boolean RSA_IS_PRESENT = ClassUtils
			.isPresent("org.springframework.security.rsa.crypto.RsaSecretEncryptor", null);

	@Override
	public void initialize(BootstrapRegistry registry) {
		if (!ClassUtils.isPresent("org.springframework.security.crypto.encrypt.TextEncryptor", null)) {
			return;
		}

		registry.registerIfAbsent(KeyProperties.class, context -> context.get(Binder.class)
				.bind(KeyProperties.PREFIX, KeyProperties.class).orElseGet(KeyProperties::new));
		if (RSA_IS_PRESENT) {
			registry.registerIfAbsent(RsaProperties.class, context -> context.get(Binder.class)
					.bind(RsaProperties.PREFIX, RsaProperties.class).orElseGet(RsaProperties::new));
		}
		TextEncryptorUtils.register(registry);

		// promote beans to context
		registry.addCloseListener(event -> {
			if (TextEncryptorUtils.isLegacyBootstrap(event.getApplicationContext().getEnvironment())) {
				return;
			}
			BootstrapContext bootstrapContext = event.getBootstrapContext();
			KeyProperties keyProperties = bootstrapContext.get(KeyProperties.class);
			ConfigurableListableBeanFactory beanFactory = event.getApplicationContext().getBeanFactory();
			if (keyProperties != null) {
				beanFactory.registerSingleton("keyProperties", keyProperties);
			}
			if (RSA_IS_PRESENT) {
				RsaProperties rsaProperties = bootstrapContext.get(RsaProperties.class);
				if (rsaProperties != null) {
					beanFactory.registerSingleton("rsaProperties", rsaProperties);
				}
			}
			TextEncryptorUtils.promote(bootstrapContext, beanFactory);
		});
	}

	@Deprecated
	public static boolean keysConfigured(KeyProperties properties) {
		return TextEncryptorUtils.keysConfigured(properties);
	}

	@Deprecated
	public static class FailsafeTextEncryptor extends TextEncryptorUtils.FailsafeTextEncryptor {

	}

}
