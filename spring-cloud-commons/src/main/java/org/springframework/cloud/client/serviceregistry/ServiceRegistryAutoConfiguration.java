package org.springframework.cloud.client.serviceregistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
public class ServiceRegistryAutoConfiguration {

	@ConditionalOnBean(ServiceRegistry.class)
	@ConditionalOnClass(Endpoint.class)
	protected class ServiceRegistryEndpointConfiguration {
		@Autowired(required = false)
		private Registration registration;

		@Bean
		@ConditionalOnEnabledEndpoint
		public ServiceRegistryEndpoint serviceRegistryEndpoint(ServiceRegistry serviceRegistry) {
			ServiceRegistryEndpoint endpoint = new ServiceRegistryEndpoint(serviceRegistry);
			endpoint.setRegistration(registration);
			return endpoint;
		}
	}
}
