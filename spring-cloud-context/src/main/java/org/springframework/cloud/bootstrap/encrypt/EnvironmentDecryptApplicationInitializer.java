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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Decrypt properties from the environment and insert them with high priority so they
 * override the encrypted values.
 *
 * @author Dave Syer
 * @author Tim Ysewyn
 */
public class EnvironmentDecryptApplicationInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	/**
	 * Name of the decrypted property source.
	 */
	public static final String DECRYPTED_PROPERTY_SOURCE_NAME = "decrypted";

	/**
	 * Name of the decrypted bootstrap property source.
	 */
	public static final String DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME = "decryptedBootstrap";

	/**
	 * Prefix indicating an encrypted value.
	 */
	public static final String ENCRYPTED_PROPERTY_PREFIX = "{cipher}";

	private static final Pattern COLLECTION_PROPERTY = Pattern
			.compile("(\\S+)?\\[(\\d+)\\](\\.\\S+)?");

	private static Log logger = LogFactory
			.getLog(EnvironmentDecryptApplicationInitializer.class);

	private int order = Ordered.HIGHEST_PRECEDENCE + 15;

	private TextEncryptor encryptor;

	private boolean failOnError = true;

	/**
	 * Strategy to determine how to handle exceptions during decryption.
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
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();

		Set<String> found = new LinkedHashSet<>();
		if (!propertySources.contains(DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
			// No reason to decrypt bootstrap twice
			PropertySource<?> bootstrap = propertySources
					.get(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME);
			if (bootstrap != null) {
				Map<String, Object> map = decrypt(bootstrap);
				if (!map.isEmpty()) {
					found.addAll(map.keySet());
					insert(applicationContext, new SystemEnvironmentPropertySource(
							DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME, map));
				}
			}
		}
		removeDecryptedProperties(applicationContext);
		Map<String, Object> map = decrypt(propertySources);
		if (!map.isEmpty()) {
			// We have some decrypted properties
			found.addAll(map.keySet());
			insert(applicationContext, new SystemEnvironmentPropertySource(
					DECRYPTED_PROPERTY_SOURCE_NAME, map));
		}
		if (!found.isEmpty()) {
			ApplicationContext parent = applicationContext.getParent();
			if (parent != null) {
				// The parent is actually the bootstrap context, and it is fully
				// initialized, so we can fire an EnvironmentChangeEvent there to rebind
				// @ConfigurationProperties, in case they were encrypted.
				parent.publishEvent(new EnvironmentChangeEvent(parent, found));
			}

		}
	}

	private void insert(ApplicationContext applicationContext,
			PropertySource<?> propertySource) {
		ApplicationContext parent = applicationContext;
		while (parent != null) {
			if (parent.getEnvironment() instanceof ConfigurableEnvironment) {
				ConfigurableEnvironment mutable = (ConfigurableEnvironment) parent
						.getEnvironment();
				insert(mutable.getPropertySources(), propertySource);
			}
			parent = parent.getParent();
		}
	}

	private void insert(MutablePropertySources propertySources,
			PropertySource<?> propertySource) {
		if (propertySources
				.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
			if (DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME
					.equals(propertySource.getName())) {
				propertySources.addBefore(
						BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME,
						propertySource);
			}
			else {
				propertySources.addAfter(
						BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME,
						propertySource);
			}
		}
		else {
			propertySources.addFirst(propertySource);
		}
	}

	private void removeDecryptedProperties(ApplicationContext applicationContext) {
		ApplicationContext parent = applicationContext;
		while (parent != null) {
			if (parent.getEnvironment() instanceof ConfigurableEnvironment) {
				((ConfigurableEnvironment) parent.getEnvironment()).getPropertySources()
						.remove(DECRYPTED_PROPERTY_SOURCE_NAME);
			}
			parent = parent.getParent();
		}
	}

	public Map<String, Object> decrypt(PropertySources propertySources) {
		Map<String, Object> properties = merge(propertySources);
		decrypt(properties);
		return properties;
	}

	private Map<String, Object> decrypt(PropertySource<?> source) {
		Map<String, Object> properties = merge(source);
		decrypt(properties);
		return properties;
	}

	private Map<String, Object> merge(PropertySources propertySources) {
		Map<String, Object> properties = new LinkedHashMap<>();
		List<PropertySource<?>> sources = new ArrayList<>();
		for (PropertySource<?> source : propertySources) {
			sources.add(0, source);
		}
		for (PropertySource<?> source : sources) {
			merge(source, properties);
		}
		return properties;
	}

	private Map<String, Object> merge(PropertySource<?> source) {
		Map<String, Object> properties = new LinkedHashMap<>();
		merge(source, properties);
		return properties;
	}

	private void merge(PropertySource<?> source, Map<String, Object> properties) {
		if (source instanceof CompositePropertySource) {

			List<PropertySource<?>> sources = new ArrayList<>(
					((CompositePropertySource) source).getPropertySources());
			Collections.reverse(sources);

			for (PropertySource<?> nested : sources) {
				merge(nested, properties);
			}

		}
		else if (source instanceof EnumerablePropertySource) {
			Map<String, Object> otherCollectionProperties = new LinkedHashMap<>();
			boolean sourceHasDecryptedCollection = false;

			EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
			for (String key : enumerable.getPropertyNames()) {
				Object property = source.getProperty(key);
				if (property != null) {
					String value = property.toString();
					if (value.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
						properties.put(key, value);
						if (COLLECTION_PROPERTY.matcher(key).matches()) {
							sourceHasDecryptedCollection = true;
						}
					}
					else if (COLLECTION_PROPERTY.matcher(key).matches()) {
						// put non-encrypted properties so merging of index properties
						// happens correctly
						otherCollectionProperties.put(key, value);
					}
					else {
						// override previously encrypted with non-encrypted property
						properties.remove(key);
					}
				}
			}
			// copy all indexed properties even if not encrypted
			if (sourceHasDecryptedCollection && !otherCollectionProperties.isEmpty()) {
				properties.putAll(otherCollectionProperties);
			}

		}
	}

	private void decrypt(Map<String, Object> properties) {
		properties.replaceAll((key, value) -> {
			String valueString = value.toString();
			if (!valueString.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
				return value;
			}
			return decrypt(key, valueString);
		});
	}

	private String decrypt(String key, String original) {
		String value = original.substring(ENCRYPTED_PROPERTY_PREFIX.length());
		try {
			value = this.encryptor.decrypt(value);
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
			if (this.failOnError) {
				throw new IllegalStateException(message, e);
			}
			return "";
		}
	}

}
