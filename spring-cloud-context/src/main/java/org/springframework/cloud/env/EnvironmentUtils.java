package org.springframework.cloud.env;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * @author Spencer Gibb
 */
public class EnvironmentUtils {
	public static Map<String, String> getSubProperties(Environment environment,
			String keyPrefix) {
		return Binder.get(environment)
				.bind(keyPrefix, Bindable.mapOf(String.class, String.class))
				.orElseGet(Collections::emptyMap);
	}
}
