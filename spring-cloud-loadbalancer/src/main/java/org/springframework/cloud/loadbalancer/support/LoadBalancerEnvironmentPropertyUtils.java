package org.springframework.cloud.loadbalancer.support;

import org.springframework.core.env.Environment;

/**
 * @author Olga Maciaszek-Sharma
 */
public final class LoadBalancerEnvironmentPropertyUtils {

	private LoadBalancerEnvironmentPropertyUtils() {
		throw new IllegalStateException("Should not instantiate a utility class");
	}

	public static boolean trueForClientOrDefault(Environment environment, String propertySuffix) {
		String defaultValue = environment
				.getProperty("spring.cloud.loadbalancer." + propertySuffix);
		String clientValue = environment
				.getProperty("spring.cloud.loadbalancer.clients." +
						LoadBalancerClientFactory
								.getName(environment) + "." + propertySuffix);
		if (clientValue != null && clientValue
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		return clientValue == null && defaultValue != null && defaultValue
				.equalsIgnoreCase(Boolean.TRUE.toString());
	}

	public static boolean trueOrMissingForClientOrDefault(Environment environment, String propertySuffix) {
		String defaultValue = environment
				.getProperty("spring.cloud.loadbalancer." + propertySuffix);
		String clientValue = environment
				.getProperty("spring.cloud.loadbalancer.clients." +
						LoadBalancerClientFactory
								.getName(environment) + "." + propertySuffix);
		if (clientValue != null && clientValue
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		if (clientValue == null && defaultValue != null && defaultValue
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		return clientValue == null && defaultValue == null;
	}
}
