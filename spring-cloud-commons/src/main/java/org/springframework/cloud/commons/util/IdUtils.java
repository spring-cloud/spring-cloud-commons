package org.springframework.cloud.commons.util;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 */
public class IdUtils {

	private static final String SEPARATOR = ":";

	public static String getDefaultInstanceId(PropertyResolver resolver) {
		RelaxedPropertyResolver relaxed = new RelaxedPropertyResolver(resolver);
		String vcapInstanceId = relaxed.getProperty("vcap.application.instance_id");
		if (StringUtils.hasText(vcapInstanceId)) {
			return vcapInstanceId;
		}

		String hostname = relaxed.getProperty("spring.cloud.client.hostname");
		String appName = relaxed.getProperty("spring.application.name");

		String namePart = combineParts(hostname, SEPARATOR, appName);

		// in a containerized environment, it makes more sense to build the
		// instance id with the mapped port than the container internal server.port
		String containerHostPort = relaxed.getProperty("spring.cloud.container.hostPort");
		String indexPart = relaxed.getProperty("spring.application.instance_id",
				containerHostPort!=null?containerHostPort:relaxed.getProperty("server.port"));


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
