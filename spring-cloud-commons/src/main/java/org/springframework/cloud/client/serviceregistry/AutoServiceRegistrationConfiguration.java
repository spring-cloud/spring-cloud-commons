package org.springframework.cloud.client.serviceregistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties(AutoServiceRegistrationProperties.class)
public class AutoServiceRegistrationConfiguration {

	@Autowired(required = false)
	private AutoServiceRegistration autoServiceRegistration;

	@Autowired
	private AutoServiceRegistrationProperties properties;

	@PostConstruct
	protected void init() {
		if (autoServiceRegistration == null && this.properties.isFailFast()) {
			throw new IllegalStateException("Auto Service Registration has been requested, but there is no AutoServiceRegistration bean");
		}
	}
}
