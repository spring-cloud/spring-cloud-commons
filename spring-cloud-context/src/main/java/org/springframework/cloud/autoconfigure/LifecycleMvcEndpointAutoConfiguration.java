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

import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentManager;
import org.springframework.cloud.context.environment.EnvironmentManagerMvcEndpoint;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.cloud.context.restart.RestartMvcEndpoint;
import org.springframework.cloud.endpoint.GenericPostableMvcEndpoint;
import org.springframework.cloud.endpoint.RefreshEndpoint;
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
	@ConditionalOnEnabledEndpoint(value = "env.post")
	public EnvironmentManagerMvcEndpoint environmentManagerEndpoint(
			EnvironmentEndpoint delegate, EnvironmentManager environment) {
		return new EnvironmentManagerMvcEndpoint(delegate, environment);
	}

	@Bean
	@ConditionalOnBean(RefreshEndpoint.class)
	public MvcEndpoint refreshMvcEndpoint(RefreshEndpoint endpoint) {
		return new GenericPostableMvcEndpoint(endpoint);
	}

	@Bean
	@ConditionalOnBean(RestartEndpoint.class)
	public RestartMvcEndpoint restartMvcEndpoint(RestartEndpoint restartEndpoint) {
		return new RestartMvcEndpoint(restartEndpoint);
	}

	@Bean
	@ConditionalOnBean(RestartEndpoint.PauseEndpoint.class)
	public MvcEndpoint pauseMvcEndpoint(RestartEndpoint.PauseEndpoint pauseEndpoint) {
		return new GenericPostableMvcEndpoint(pauseEndpoint);
	}

	@Bean
	@ConditionalOnBean(RestartEndpoint.ResumeEndpoint.class)
	public MvcEndpoint resumeMvcEndpoint(RestartEndpoint.ResumeEndpoint resumeEndpoint) {
		return new GenericPostableMvcEndpoint(resumeEndpoint);
	}

}
