package org.springframework.cloud.env;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Spencer Gibb
 */
public class EnvironmentUtils {
	public static Map<String, Object> getSubProperties(Environment environment, String keyPrefix) {
		if (environment instanceof ConfigurableEnvironment) {
			ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
			Map<String, Object> subProperties = new LinkedHashMap<>();
			PropertySources propertySources = env.getPropertySources();
			for (PropertySource<?> source : propertySources) {
				if (source instanceof EnumerablePropertySource) {
					for (String name : ((EnumerablePropertySource<?>) source)
							.getPropertyNames()) {
						String key = getSubKey(name, keyPrefix);
						if (key != null && !subProperties.containsKey(key)) {
							subProperties.put(key, source.getProperty(name));
						}
					}
				}
			}
			return Collections.unmodifiableMap(subProperties);
		}
		return Collections.emptyMap();
	}

	private static String getSubKey(String name, String keyPrefix) {
		if (name.startsWith(keyPrefix)) {
			return name.substring(keyPrefix.length());
		}
		return null;
	}
}
