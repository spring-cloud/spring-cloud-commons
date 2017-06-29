package org.springframework.cloud.client.discovery.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("spring.cloud.discovery.client.health-indicator")
public class DiscoveryClientHealthIndicatorProperties {

	private boolean enabled = true;

	private boolean includeDescription = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isIncludeDescription() {
		return includeDescription;
	}

	public void setIncludeDescription(boolean includeDescription) {
		this.includeDescription = includeDescription;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("DiscoveryClientHealthIndicatorProperties{");
		sb.append("enabled=").append(enabled);
		sb.append(", includeDescription=").append(includeDescription);
		sb.append('}');
		return sb.toString();
	}
}
