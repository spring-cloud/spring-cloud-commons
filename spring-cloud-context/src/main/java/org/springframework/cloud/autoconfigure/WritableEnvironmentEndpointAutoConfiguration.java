package org.springframework.cloud.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointProperties;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.environment.EnvironmentManager;
import org.springframework.cloud.context.environment.WritableEnvironmentEndpoint;
import org.springframework.cloud.context.environment.WritableEnvironmentEndpointWebExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link WritableEnvironmentEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ EnvironmentEndpoint.class, EnvironmentEndpointProperties.class })
@ConditionalOnBean(EnvironmentManager.class)
@AutoConfigureBefore(EnvironmentEndpointAutoConfiguration.class)
@AutoConfigureAfter(LifecycleMvcEndpointAutoConfiguration.class)
@EnableConfigurationProperties({ EnvironmentEndpointProperties.class })
public class WritableEnvironmentEndpointAutoConfiguration {

	private final EnvironmentEndpointProperties properties;

	public WritableEnvironmentEndpointAutoConfiguration(
			EnvironmentEndpointProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public WritableEnvironmentEndpoint environmentEndpoint(Environment environment) {
		WritableEnvironmentEndpoint endpoint = new WritableEnvironmentEndpoint(environment);
		String[] keysToSanitize = this.properties.getKeysToSanitize();
		if (keysToSanitize != null) {
			endpoint.setKeysToSanitize(keysToSanitize);
		}
		return endpoint;
	}

	@Bean
	@ConditionalOnEnabledEndpoint
	public WritableEnvironmentEndpointWebExtension environmentWebEndpointExtension(
			WritableEnvironmentEndpoint endpoint, EnvironmentManager environment) {
		return new WritableEnvironmentEndpointWebExtension(endpoint, environment);
	}

}
