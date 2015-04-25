package org.springframework.cloud.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.config")
public class PropertySourceBootstrapProperties {

	/**
	 * Flag to indicate that the external properties should override system properties.
	 * Default true.
	 */
	private boolean overrideSystemProperties = true;

	/**
	 * Flag to indicate that the external properties shouldn't override any other specified 
	 * property sources
	 * Default false.
	 */
	private boolean overrideNoProperties = false;

	/**
	 * Flag to indicate that {@link #isSystemPropertiesOverride()
	 * systemPropertiesOverride} and {@link #isOverrideNoProperties() overrideNoProperties} can be used.
	 * Set to false to prevent users from changing the default accidentally. Default true.
	 */
	private boolean allowOverride = true;

	public boolean isOverrideSystemProperties() {
		return overrideSystemProperties;
	}

	public void setOverrideSystemProperties(boolean overrideSystemProperties) {
		this.overrideSystemProperties = overrideSystemProperties;
	}
	
	public boolean isOverrideNoProperties() {
		return overrideNoProperties;
	}
	
	public void setOverrideNoProperties(boolean overrideNoProperties) {
		this.overrideNoProperties = overrideNoProperties;
	}

	public boolean isAllowOverride() {
		return allowOverride;
	}

	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

}
