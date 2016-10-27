package org.springframework.cloud.client.serviceregistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author Spencer Gibb
 */
@Configuration
public class AutoServiceRegistrationConfiguration {

	@Autowired(required = false)
	private AutoServiceRegistration autoServiceRegistration;

	@PostConstruct
	protected void init() {
		boolean failFast = false; // TODO: move to configuration props
		if (autoServiceRegistration == null && failFast) {
			throw new IllegalStateException("Auto Service Registration has been requested, but there is no AutoServiceRegistration bean");
		}
	}
}
