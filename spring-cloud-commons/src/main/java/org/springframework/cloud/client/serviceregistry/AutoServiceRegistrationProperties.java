package org.springframework.cloud.client.serviceregistry;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("spring.cloud.service-registry.auto-registration")
public class AutoServiceRegistrationProperties {

	/** Should startup fail if there is no AutoServiceRegistration, default to false. */
	private boolean failFast = false;

	public boolean isFailFast() {
		return failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}
}
