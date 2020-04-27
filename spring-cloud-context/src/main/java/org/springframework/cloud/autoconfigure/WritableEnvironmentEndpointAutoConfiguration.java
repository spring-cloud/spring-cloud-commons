/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointProperties;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ EnvironmentEndpoint.class, EnvironmentEndpointProperties.class })
@ConditionalOnBean(EnvironmentManager.class)
@AutoConfigureBefore(EnvironmentEndpointAutoConfiguration.class)
@AutoConfigureAfter(LifecycleMvcEndpointAutoConfiguration.class)
@EnableConfigurationProperties({ EnvironmentEndpointProperties.class })
@ConditionalOnProperty("management.endpoint.env.post.enabled")
public class WritableEnvironmentEndpointAutoConfiguration {

	private final EnvironmentEndpointProperties properties;

	public WritableEnvironmentEndpointAutoConfiguration(
			EnvironmentEndpointProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	public WritableEnvironmentEndpoint writableEnvironmentEndpoint(
			Environment environment) {
		WritableEnvironmentEndpoint endpoint = new WritableEnvironmentEndpoint(
				environment);
		String[] keysToSanitize = this.properties.getKeysToSanitize();
		if (keysToSanitize != null) {
			endpoint.setKeysToSanitize(keysToSanitize);
		}
		return endpoint;
	}

	@Bean
	@ConditionalOnAvailableEndpoint
	public WritableEnvironmentEndpointWebExtension writableEnvironmentEndpointWebExtension(
			WritableEnvironmentEndpoint endpoint, EnvironmentManager environment) {
		return new WritableEnvironmentEndpointWebExtension(endpoint, environment);
	}

}
