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

package org.springframework.cloud.bootstrap.encrypt;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.bootstrap.TextEncryptorConfigBootstrapper;
import org.springframework.cloud.bootstrap.TextEncryptorConfigBootstrapper.FailsafeTextEncryptor;
import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.ClassUtils;

import static org.springframework.cloud.bootstrap.TextEncryptorConfigBootstrapper.keysConfigured;
import static org.springframework.cloud.util.PropertyUtils.bootstrapEnabled;
import static org.springframework.cloud.util.PropertyUtils.useLegacyProcessing;

/**
 * Decrypt properties from the environment and insert them with high priority so they
 * override the encrypted values.
 *
 * @author Dave Syer
 * @author Tim Ysewyn
 */
public class DecryptEnvironmentPostProcessor extends AbstractEnvironmentDecrypt
		implements EnvironmentPostProcessor, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (bootstrapEnabled(environment) || useLegacyProcessing(environment)) {
			return;
		}

		MutablePropertySources propertySources = environment.getPropertySources();

		environment.getPropertySources().remove(DECRYPTED_PROPERTY_SOURCE_NAME);

		TextEncryptor encryptor = getTextEncryptor(environment);

		Map<String, Object> map = decrypt(encryptor, propertySources);
		if (!map.isEmpty()) {
			// We have some decrypted properties
			propertySources.addFirst(new SystemEnvironmentPropertySource(DECRYPTED_PROPERTY_SOURCE_NAME, map));
		}

	}

	protected TextEncryptor getTextEncryptor(ConfigurableEnvironment environment) {
		Binder binder = Binder.get(environment);
		KeyProperties keyProperties = binder.bind(KeyProperties.PREFIX, KeyProperties.class)
				.orElseGet(KeyProperties::new);
		if (keysConfigured(keyProperties)) {

			if (ClassUtils.isPresent("org.springframework.security.rsa.crypto.RsaSecretEncryptor", null)) {
				RsaProperties rsaProperties = binder.bind(RsaProperties.PREFIX, RsaProperties.class)
						.orElseGet(RsaProperties::new);
				return TextEncryptorConfigBootstrapper.rsaTextEncryptor(keyProperties, rsaProperties);
			}
			return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
		}
		// no keys configured
		return new FailsafeTextEncryptor();
	}

}
