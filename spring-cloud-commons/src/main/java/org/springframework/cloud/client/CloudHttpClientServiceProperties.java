package org.springframework.cloud.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Olga Maciaszek-Sharma
 */
@ConfigurationProperties("spring.cloud.http.client.service")
public class CloudHttpClientServiceProperties extends AbstractCloudHttpClientServiceProperties {

	private Map<String, Group> group = new LinkedHashMap<>();

	public Map<String, Group> getGroup() {
		return this.group;
	}

	public void setGroup(Map<String, Group> group) {
		this.group = group;
	}

	/**
	 * Properties for a single HTTP Service client group.
	 */
	public static class Group extends AbstractCloudHttpClientServiceProperties {

	}
}
