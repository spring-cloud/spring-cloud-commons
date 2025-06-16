package org.springframework.cloud.client;

/**
 * @author Olga Maciaszek-Sharma
 */
public abstract class AbstractCloudHttpClientServiceProperties {

	private String fallbackClass;

	public String getFallbackClass() {
		return fallbackClass;
	}

	public void setFallbackClass(String fallbackClass) {
		this.fallbackClass = fallbackClass;
	}
}
