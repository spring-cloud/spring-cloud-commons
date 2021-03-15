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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ TextEncryptor.class })
@EnableConfigurationProperties
public class EncryptionBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public KeyProperties keyProperties() {
		return new KeyProperties();
	}

	@Bean
	public EnvironmentDecryptApplicationInitializer environmentDecryptApplicationListener(
			ConfigurableApplicationContext context, KeyProperties keyProperties) {
		TextEncryptor encryptor;
		try {
			encryptor = context.getBean(TextEncryptor.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			encryptor = new TextEncryptorUtils.FailsafeTextEncryptor();
		}
		EnvironmentDecryptApplicationInitializer listener = new EnvironmentDecryptApplicationInitializer(encryptor);
		listener.setFailOnError(keyProperties.isFailOnError());
		return listener;
	}

	@Deprecated
	public static TextEncryptor createTextEncryptor(KeyProperties keyProperties, RsaProperties rsaProperties) {
		return TextEncryptorUtils.createTextEncryptor(keyProperties, rsaProperties);
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(KeyCondition.class)
	@ConditionalOnClass(RsaSecretEncryptor.class)
	@EnableConfigurationProperties
	protected static class RsaEncryptionConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RsaProperties rsaProperties() {
			return new RsaProperties();
		}

		@Bean
		@ConditionalOnMissingBean(TextEncryptor.class)
		public TextEncryptor textEncryptor(KeyProperties keyProperties, RsaProperties rsaProperties) {
			return TextEncryptorUtils.createTextEncryptor(keyProperties, rsaProperties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(KeyCondition.class)
	@ConditionalOnMissingClass("org.springframework.security.rsa.crypto.RsaSecretEncryptor")
	protected static class VanillaEncryptionConfiguration {

		@Autowired
		private KeyProperties key;

		@Bean
		@ConditionalOnMissingBean(TextEncryptor.class)
		public TextEncryptor textEncryptor() {
			return new EncryptorFactory(this.key.getSalt()).create(this.key.getKey());
		}

	}

	/**
	 * A Spring Boot condition for key encryption.
	 */
	public static class KeyCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			if (hasProperty(environment, "encrypt.key-store.location")) {
				if (hasProperty(environment, "encrypt.key-store.password")) {
					return ConditionOutcome.match("Keystore found in Environment");
				}
				return ConditionOutcome.noMatch("Keystore found but no password in Environment");
			}
			else if (hasProperty(environment, "encrypt.key")) {
				return ConditionOutcome.match("Key found in Environment");
			}
			return ConditionOutcome.noMatch("Keystore nor key found in Environment");
		}

		private boolean hasProperty(Environment environment, String key) {
			String value = environment.getProperty(key);
			if (value == null) {
				return false;
			}
			return StringUtils.hasText(environment.resolvePlaceholders(value));
		}

	}

	@Deprecated
	protected static class FailsafeTextEncryptor extends TextEncryptorUtils.FailsafeTextEncryptor {

	}

}
