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
	 * Flag to indicate that {@link #isOverrideSystemProperties()
	 * systemPropertiesOverride} can be used. Set to false to prevent users from changing
	 * the default accidentally. Default true.
	 */
	private boolean allowOverride = true;

	/**
	 * Flag to indicate that when {@link #setAllowOverride(boolean) allowOverride} is
	 * true, external properties should take lowest priority, and not override any
	 * existing property sources (including local config files). Default false.
	 */
	private boolean overrideNone = false;

	public boolean isOverrideNone() {
		return this.overrideNone;
	}

	public void setOverrideNone(boolean overrideNone) {
		this.overrideNone = overrideNone;
	}

	public boolean isOverrideSystemProperties() {
		return this.overrideSystemProperties;
	}

	public void setOverrideSystemProperties(boolean overrideSystemProperties) {
		this.overrideSystemProperties = overrideSystemProperties;
	}

	public boolean isAllowOverride() {
		return this.allowOverride;
	}

	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

}
