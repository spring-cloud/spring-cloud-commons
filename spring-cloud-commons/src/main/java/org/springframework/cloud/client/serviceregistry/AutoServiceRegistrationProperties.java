package org.springframework.cloud.client.serviceregistry;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("spring.cloud.service-registry.auto-registration")
@Getter
@Setter
public class AutoServiceRegistrationProperties {

	/** If Auto-Service Registration is enabled, default to true. */
	private boolean enabled = true;

	/** Whether to register the management as a service, defaults to true */
	private boolean registerManagement = true;

	/** Should startup fail if there is no AutoServiceRegistration, default to false. */
	private boolean failFast = false;

}
