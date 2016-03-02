package org.springframework.cloud.commons.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties(InetUtilsProperties.PREFIX)
public class InetUtilsProperties {
	public static final String PREFIX = "spring.cloud.inetutils";

	/**
	 * The default hostname. Used in case of errors.
	 */
	private String defaultHostname = "localhost";

	/**
	 * The default ipaddress. Used in case of errors.
	 */
	private String defaultIpAddress = "127.0.0.1";

	/**
	 * Timeout in seconds for calculating hostname.
	 */
	@Value("${spring.util.timeout.sec:${SPRING_UTIL_TIMEOUT_SEC:1}}")
	private int timeoutSeconds = 1;

	/**
	 * List of Java regex expressions for network interfaces that will be ignored.
	 */
	private List<String> ignoredInterfaces = new ArrayList<>();
}
