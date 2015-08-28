/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Decrypt properties from the environment and insert them with high priority so they
 * override the encrypted values.
 * 
 * @author Dave Syer
 *
 */
public class EnvironmentDecryptApplicationInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	public static final String DECRYPTED_PROPERTY_SOURCE_NAME = "decrypted";

	private int order = Ordered.HIGHEST_PRECEDENCE + 15;

	private static Log logger = LogFactory
			.getLog(EnvironmentDecryptApplicationInitializer.class);

	private TextEncryptor encryptor;

	private boolean failOnError = true;

	/**
	 * Strategy to determine how to handle exceptions during decryption.
	 * 
	 * @param failOnError the flag value (default true)
	 */
	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	public EnvironmentDecryptApplicationInitializer(TextEncryptor encryptor) {
		this.encryptor = encryptor;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		MapPropertySource decrypted = new MapPropertySource(
				DECRYPTED_PROPERTY_SOURCE_NAME, decrypt(environment.getPropertySources()));
		if (!decrypted.getSource().isEmpty()) {
			// We have some decrypted properties
			insert(environment.getPropertySources(), decrypted);
			ApplicationContext parent = applicationContext.getParent();
			if (parent != null
					&& (parent.getEnvironment() instanceof ConfigurableEnvironment)) {
				ConfigurableEnvironment mutable = (ConfigurableEnvironment) parent
						.getEnvironment();
				// The parent is actually the bootstrap context, and it is fully
				// initialized, so we can fire an EnvironmentChangeEvent there to rebind
				// @ConfigurationProperties, in case they were encrypted.
				insert(mutable.getPropertySources(), decrypted);
				parent.publishEvent(new EnvironmentChangeEvent(decrypted.getSource()
						.keySet()));
			}
		}
	}

	private void insert(MutablePropertySources propertySources,
			MapPropertySource propertySource) {
		if (propertySources
				.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
			propertySources.addAfter(
					BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME,
					propertySource);
		}
		else {
			propertySources.addFirst(propertySource);
		}
	}

	public Map<String, Object> decrypt(PropertySources propertySources) {
		Map<String, Object> overrides = new LinkedHashMap<String, Object>();
		for (PropertySource<?> source : propertySources) {
			decrypt(source, overrides);
		}
		return overrides;
	}

	private void decrypt(PropertySource<?> source, Map<String, Object> overrides) {

		if (source instanceof EnumerablePropertySource) {

			EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
			for (String key : enumerable.getPropertyNames()) {
				Object property = source.getProperty(key);
				if (property != null) {
					String value = property.toString();
					if (value.startsWith("{cipher}")) {
						value = value.substring("{cipher}".length());
						try {
							value = encryptor.decrypt(value);
							if (logger.isDebugEnabled()) {
								logger.debug("Decrypted: key=" + key);
							}
						} catch (Exception e) {
							String message = "Cannot decrypt: key=" + key;
							if (failOnError) {
								throw new IllegalStateException(message, e);
							}
							if (logger.isDebugEnabled()) {
								logger.warn(message, e);
							} else {
								logger.warn(message);
							}
							// Set value to empty to avoid making a password out of the
							// cipher text
							value = "";
						}
						overrides.put(key, value);
					}
				}
			}

		}
		else if (source instanceof CompositePropertySource) {

			for (PropertySource<?> nested : ((CompositePropertySource) source)
					.getPropertySources()) {
				decrypt(nested, overrides);
			}

		}

	}
	
}
