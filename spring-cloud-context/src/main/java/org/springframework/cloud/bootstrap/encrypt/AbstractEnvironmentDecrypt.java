/*
 * Copyright 2013-present the original author or authors.
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Abstract class that handles decrypting and merging of PropertySources.
 */
public abstract class AbstractEnvironmentDecrypt {

	private static final Pattern COLLECTION_PROPERTY = Pattern.compile("(\\S+)?\\[(\\d+)\\](\\.\\S+)?");

	/**
	 * Name of the decrypted property source.
	 */
	public static final String DECRYPTED_PROPERTY_SOURCE_NAME = "decrypted";

	/**
	 * Prefix indicating an encrypted value.
	 */
	public static final String ENCRYPTED_PROPERTY_PREFIX = "{cipher}";

	protected Log logger = LogFactory.getLog(getClass());

	private boolean failOnError = true;

	/**
	 * Strategy to determine how to handle exceptions during decryption.
	 * @param failOnError the flag value (default true)
	 */
	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	public boolean isFailOnError() {
		return this.failOnError;
	}

	protected Map<String, Object> decrypt(TextEncryptor encryptor, PropertySources propertySources) {
		Map<String, Object> decryptedProperties = new LinkedHashMap<>();
		var visitor = new PropertyVisitor();

		for (PropertySource<?> propertySource : propertySources) {
			if (propertySource instanceof EnumerablePropertySource<?> enumerable) {
				for (String propertyName : enumerable.getPropertyNames()) {
					if (visitor.isVisited(propertyName)) {
						continue;
					}

					var collectionMatcher = COLLECTION_PROPERTY.matcher(propertyName);
					if (collectionMatcher.matches()) {
						// It is an indexed property. All items should be checked.
						var prefix = collectionMatcher.group(1);
						if (prefix == null) {
							prefix = "";
						}
						var indexed = getPropertyValues(enumerable, encryptor, prefix);
						// Include only if contains decrypted values
						if (indexed.containsDecrypted) {
							decryptedProperties.putAll(indexed.values);
						}
						visitor.visited(indexed.values.keySet());
					}
					else {
						var single = getPropertyValue(enumerable, encryptor, propertyName);
						// Include only if decrypted
						if (single.isDecrypted) {
							decryptedProperties.put(propertyName, single.value);
						}
						visitor.visited(propertyName);
					}
				}
			}
		}

		return decryptedProperties;
	}

	private IndexedValue getPropertyValues(EnumerablePropertySource<?> source, TextEncryptor encryptor, String prefix) {
		boolean containsDecrypted = false;
		Map<String, Object> elements = new HashMap<>();
		for (String name : source.getPropertyNames()) {
			if (COLLECTION_PROPERTY.matcher(name).matches() && name.startsWith(prefix)) {
				var value = getPropertyValue(source, encryptor, name);
				elements.put(name, value.value);
				if (value.isDecrypted) {
					containsDecrypted = true;
				}
			}
		}

		return new IndexedValue(elements, containsDecrypted);
	}

	private SingleValue getPropertyValue(PropertySource<?> source, TextEncryptor encryptor, String name) {
		var value = source.getProperty(name);
		if (value != null) {
			var valueString = value.toString();
			if (valueString.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
				return new SingleValue(this.decrypt(encryptor, name, valueString), true);
			}
		}
		return new SingleValue(value, false);
	}

	private String decrypt(TextEncryptor encryptor, String key, String original) {
		String value = original.substring(ENCRYPTED_PROPERTY_PREFIX.length());
		try {
			value = encryptor.decrypt(value);
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

	private record SingleValue(Object value, boolean isDecrypted) {
	}

	private record IndexedValue(Map<String, Object> values, boolean containsDecrypted) {
	}

	private static final class PropertyVisitor {

		/**
		 * Using SystemEnvironmentPropertySource, instead of a simple Map, just to cover
		 * relaxed-binding cases.
		 * <p>
		 * See {@link SystemEnvironmentPropertySource#containsProperty(String) } for more
		 * details.
		 */
		private final SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource("visitor",
				new HashMap<>());

		boolean isVisited(String name) {
			return this.propertySource.containsProperty(name);
		}

		void visited(String name) {
			propertySource.getSource().put(name, "");
		}

		void visited(Set<String> names) {
			for (String name : names) {
				propertySource.getSource().put(name, "");
			}
		}

	}

}
