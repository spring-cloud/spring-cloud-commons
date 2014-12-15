package org.springframework.cloud.client.discovery;

import org.springframework.boot.actuate.health.Health;

/**
 * @author Spencer Gibb
 */
public interface DiscoveryHealthIndicator {
	public String getName();

	/**
	 * @return an indication of health
	 */
	Health health();
}
