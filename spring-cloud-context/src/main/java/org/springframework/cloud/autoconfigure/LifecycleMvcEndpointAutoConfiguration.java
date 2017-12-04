/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentManager;
import org.springframework.cloud.context.environment.EnvironmentWebEndpointExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for some MVC endpoints governing the application context lifecycle.
 * Provides restart, pause, resume, refresh (environment) and environment update
 * endpoints.
 *
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnClass(EnvironmentEndpoint.class)
@ConditionalOnWebApplication
@AutoConfigureAfter({ WebMvcAutoConfiguration.class,
		RefreshEndpointAutoConfiguration.class })
public class LifecycleMvcEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(EnvironmentEndpoint.class)
	@ConditionalOnEnabledEndpoint
	public EnvironmentWebEndpointExtension environmentWebEndpointExtension(
			EnvironmentManager environment) {
		return new EnvironmentWebEndpointExtension(environment);
	}

}
