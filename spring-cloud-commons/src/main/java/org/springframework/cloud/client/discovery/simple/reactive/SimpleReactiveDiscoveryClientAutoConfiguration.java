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

package org.springframework.cloud.client.discovery.simple.reactive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryHealthIndicatorEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ReactiveCommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Spring Boot auto-configuration for simple properties-based reactive discovery client.
 *
 * @author Tim Ysewyn
 * @author Charu Covindane
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@EnableConfigurationProperties(DiscoveryClientHealthIndicatorProperties.class)
@AutoConfigureBefore(ReactiveCommonsClientAutoConfiguration.class)
@AutoConfigureAfter(ReactiveCompositeDiscoveryClientAutoConfiguration.class)
public class SimpleReactiveDiscoveryClientAutoConfiguration implements ApplicationListener<WebServerInitializedEvent> {

	@Autowired(required = false)
	private ServerProperties server;

	@Value("${spring.application.name:application}")
	private String serviceId;

	@Autowired
	private InetUtils inet;

	private int port = 0;

	private SimpleReactiveDiscoveryProperties simple = new SimpleReactiveDiscoveryProperties();

	@Bean
	public SimpleReactiveDiscoveryProperties simpleReactiveDiscoveryProperties() {
		simple.getLocal().setServiceId(serviceId);
		simple.getLocal().setHost(inet.findFirstNonLoopbackHostInfo().getHostname());
		simple.getLocal().setPort(findPort());
		return simple;
	}

	@Bean
	@Order
	public SimpleReactiveDiscoveryClient simpleReactiveDiscoveryClient(SimpleReactiveDiscoveryProperties properties) {
		return new SimpleReactiveDiscoveryClient(properties);
	}

	private int findPort() {
		if (port > 0) {
			return port;
		}
		if (server != null && server.getPort() != null && server.getPort() > 0) {
			return server.getPort();
		}
		return 8080;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
		port = webServerInitializedEvent.getWebServer().getPort();
		if (port > 0) {
			simple.getLocal().setHost(inet.findFirstNonLoopbackHostInfo().getHostname());
			simple.getLocal().setPort(port);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ReactiveHealthIndicator.class)
	protected static class HealthConfiguration {

		@Bean
		@ConditionalOnDiscoveryHealthIndicatorEnabled
		public ReactiveDiscoveryClientHealthIndicator simpleReactiveDiscoveryClientHealthIndicator(
				DiscoveryClientHealthIndicatorProperties properties,
				SimpleReactiveDiscoveryClient simpleReactiveDiscoveryClient) {
			return new ReactiveDiscoveryClientHealthIndicator(simpleReactiveDiscoveryClient, properties);
		}

	}

}
