package org.springframework.cloud.client.serviceregistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@ConditionalOnBean(ServiceRegistry.class)
@Configuration
public class ServiceRegistryAutoConfiguration {

	@Autowired(required = false)
	private Registration registration;

	@ConditionalOnClass(Endpoint.class)
	@Bean
	public ServiceRegistryEndpoint serviceRegistryEndpoint(ServiceRegistry serviceRegistry) {
		ServiceRegistryEndpoint endpoint = new ServiceRegistryEndpoint(serviceRegistry);
		endpoint.setRegistration(registration);
		return endpoint;
	}
}
