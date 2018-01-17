package org.springframework.cloud.commons.util;

import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 */
public class IdUtils {

	private static final String SEPARATOR = ":";

	public static String getDefaultInstanceId(PropertyResolver resolver) {
	   	return getDefaultInstanceId(resolver, true);
    }

    public static String getDefaultInstanceId(PropertyResolver resolver, boolean includeHostname) {
		String vcapInstanceId = resolver.getProperty("vcap.application.instance_id");
		if (StringUtils.hasText(vcapInstanceId)) {
			return vcapInstanceId;
		}

		String hostname = null;
		if (includeHostname) {
			hostname = resolver.getProperty("spring.cloud.client.hostname");
		}
		String appName = resolver.getProperty("spring.application.name");

		String namePart = combineParts(hostname, SEPARATOR, appName);

		String indexPart = resolver.getProperty("spring.application.instance_id",
				resolver.getProperty("server.port"));

		return combineParts(namePart, SEPARATOR, indexPart);
	}

	public static String combineParts(String firstPart, String separator, String secondPart) {
		String combined = null;
		if (firstPart != null && secondPart != null) {
			combined = firstPart + separator + secondPart;
		} else if (firstPart != null) {
			combined = firstPart;
		} else if (secondPart != null) {
			combined = secondPart;
		}
		return combined;
	}

}
