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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.cloud.endpoint.event.RefreshEventListener;
import org.springframework.cloud.health.RefreshScopeHealthIndicator;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.monitor.IntegrationMBeanExporter;

@Configuration
@ConditionalOnClass(Endpoint.class)
@AutoConfigureAfter(EndpointAutoConfiguration.class)
public class RefreshEndpointAutoConfiguration {

	@ConditionalOnBean(EndpointAutoConfiguration.class)
	@Bean
	InfoEndpointRebinderConfiguration infoEndpointRebinderConfiguration() {
		return new InfoEndpointRebinderConfiguration();
	}

	@ConditionalOnMissingBean
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
	public RestartEndpoint.ResumeEndpoint resumeEndpoint(RestartEndpoint restartEndpoint) {
		return restartEndpoint.getResumeEndpoint();
	}

	@Configuration
	@ConditionalOnProperty(value = "endpoints.refresh.enabled", matchIfMissing = true)
	@ConditionalOnBean(PropertySourceBootstrapConfiguration.class)
	protected static class RefreshEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RefreshEndpoint refreshEndpoint(ConfigurableApplicationContext context,
				RefreshScope scope) {
			RefreshEndpoint endpoint = new RefreshEndpoint(context, scope);
			return endpoint;
		}

		@Bean
		public RefreshEventListener refreshEventListener(RefreshEndpoint refreshEndpoint) {
			return new RefreshEventListener(refreshEndpoint);
		}

	}

	private static class InfoEndpointRebinderConfiguration
			implements ApplicationListener<EnvironmentChangeEvent>, BeanPostProcessor {

		@Autowired
		private ConfigurableEnvironment environment;

		private Map<String, Object> map = new LinkedHashMap<String, Object>();

		@Override
		public void onApplicationEvent(EnvironmentChangeEvent event) {
			for (String key : event.getKeys()) {
				if (key.startsWith("info.")) {
					this.map.put(key.substring("info.".length()),
							this.environment.getProperty(key));
				}
			}
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof InfoEndpoint) {
				return infoEndpoint((InfoEndpoint) bean);
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		private InfoEndpoint infoEndpoint(InfoEndpoint endpoint) {
			return new InfoEndpoint(endpoint.invoke()) {
				@Override
				public Map<String, Object> invoke() {
					Map<String, Object> info = new LinkedHashMap<String, Object>(
							super.invoke());
					info.putAll(InfoEndpointRebinderConfiguration.this.map);
					return info;
				}
			};
		}

	}

}