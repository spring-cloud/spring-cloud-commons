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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.restart.PauseHandler;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.cloud.health.RefreshScopeHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.monitor.IntegrationMBeanExporter;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Venil Noronha
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ EndpointAutoConfiguration.class, Health.class })
@AutoConfigureAfter({ LifecycleMvcEndpointAutoConfiguration.class,
		RefreshAutoConfiguration.class })
@Import({ RestartEndpointWithIntegrationConfiguration.class,
		RestartEndpointWithoutIntegrationConfiguration.class,
		PauseResumeEndpointsConfiguration.class })
public class RefreshEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledHealthIndicator("refresh")
	RefreshScopeHealthIndicator refreshScopeHealthIndicator(
			ObjectProvider<RefreshScope> scope,
			ConfigurationPropertiesRebinder rebinder) {
		return new RefreshScopeHealthIndicator(scope, rebinder);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(PropertySourceBootstrapConfiguration.class)
	protected static class RefreshEndpointConfiguration {

		@Bean
		@ConditionalOnBean(ContextRefresher.class)
		@ConditionalOnAvailableEndpoint
		@ConditionalOnMissingBean
		public RefreshEndpoint refreshEndpoint(ContextRefresher contextRefresher) {
			return new RefreshEndpoint(contextRefresher);
		}

	}

}

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(IntegrationMBeanExporter.class)
class RestartEndpointWithIntegrationConfiguration {

	private IntegrationMBeanExporter exporter;

	RestartEndpointWithIntegrationConfiguration(
			@Autowired(required = false) IntegrationMBeanExporter exporter) {
		this.exporter = exporter;
	}

	@Bean
	public PauseHandler integrationPauseHandler(ObjectProvider<Pausable> pausables) {
		return new IntegrationPauseHandler(
				pausables.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnMissingBean
	public RestartEndpoint restartEndpoint() {
		RestartEndpoint endpoint = new RestartEndpoint();
		if (this.exporter != null) {
			endpoint.setIntegrationMBeanExporter(this.exporter);
		}
		return endpoint;
	}

	private class IntegrationPauseHandler implements PauseHandler {

		private List<Pausable> pausables = new ArrayList<>();

		IntegrationPauseHandler(List<Pausable> pausables) {
			this.pausables.addAll(pausables);
		}

		@Override
		public void pause() {
			for (Pausable pausable : this.pausables) {
				pausable.pause();
			}
		}

		@Override
		public void resume() {
			for (int i = this.pausables.size(); i-- > 0;) {
				this.pausables.get(i).resume();
			}
		}

	}

}

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingClass("org.springframework.integration.monitor.IntegrationMBeanExporter")
class RestartEndpointWithoutIntegrationConfiguration {

	@Bean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnMissingBean
	public RestartEndpoint restartEndpointWithoutIntegration() {
		return new RestartEndpoint();
	}

}

@Configuration(proxyBeanMethods = false)
class PauseResumeEndpointsConfiguration {

	@Bean
	@ConditionalOnBean(RestartEndpoint.class)
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	public RestartEndpoint.PauseEndpoint pauseEndpoint(RestartEndpoint restartEndpoint) {
		return restartEndpoint.getPauseEndpoint();
	}

	@Bean
	@ConditionalOnBean(RestartEndpoint.class)
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	public RestartEndpoint.ResumeEndpoint resumeEndpoint(
			RestartEndpoint restartEndpoint) {
		return restartEndpoint.getResumeEndpoint();
	}

}
