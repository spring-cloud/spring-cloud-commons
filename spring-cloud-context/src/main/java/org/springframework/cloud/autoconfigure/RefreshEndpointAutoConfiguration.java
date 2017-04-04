/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.cloud.health.RefreshScopeHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.monitor.IntegrationMBeanExporter;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Venil Noronha
 */
@Configuration
@ConditionalOnClass(Endpoint.class)
@AutoConfigureAfter(EndpointAutoConfiguration.class)
public class RefreshEndpointAutoConfiguration {

	@ConditionalOnMissingBean
	@ConditionalOnEnabledHealthIndicator("refresh")
	@Bean
	RefreshScopeHealthIndicator refreshScopeHealthIndicator(RefreshScope scope,
			ConfigurationPropertiesRebinder rebinder) {
		return new RefreshScopeHealthIndicator(scope, rebinder);
	}

	@ConditionalOnClass(IntegrationMBeanExporter.class)
	protected static class RestartEndpointWithIntegration {

		@Autowired(required = false)
		private IntegrationMBeanExporter exporter;

		@Bean
		@ConditionalOnMissingBean
		public RestartEndpoint restartEndpoint() {
			RestartEndpoint endpoint = new RestartEndpoint();
			if (this.exporter != null) {
				endpoint.setIntegrationMBeanExporter(this.exporter);
			}
			return endpoint;
		}

	}

	@ConditionalOnMissingClass("org.springframework.integration.monitor.IntegrationMBeanExporter")
	protected static class RestartEndpointWithoutIntegration {

		@Bean
		@ConditionalOnMissingBean
		public RestartEndpoint restartEndpoint() {
			return new RestartEndpoint();
		}
	}

	@Bean
	@ConfigurationProperties("endpoints.pause")
	public RestartEndpoint.PauseEndpoint pauseEndpoint(RestartEndpoint restartEndpoint) {
		return restartEndpoint.getPauseEndpoint();
	}

	@Bean
	@ConfigurationProperties("endpoints.resume")
	public RestartEndpoint.ResumeEndpoint resumeEndpoint(
			RestartEndpoint restartEndpoint) {
		return restartEndpoint.getResumeEndpoint();
	}

	@Configuration
	@ConditionalOnProperty(value = "endpoints.refresh.enabled", matchIfMissing = true)
	@ConditionalOnBean(PropertySourceBootstrapConfiguration.class)
	protected static class RefreshEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RefreshEndpoint refreshEndpoint(ContextRefresher contextRefresher) {
			RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
			return endpoint;
		}

	}
}