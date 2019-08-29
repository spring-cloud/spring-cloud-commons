/*
 * Copyright 2012-2019 the original author or authors.
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

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryHealthIndicator;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Spring Boot auto-configuration for simple properties-based reactive discovery client.
 *
 * @author Tim Ysewyn
 */
@Configuration
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties(DiscoveryClientHealthIndicatorProperties.class)
public class SimpleReactiveDiscoveryClientAutoConfiguration {

	@Autowired(required = false)
	private ServerProperties server;

	@Value("${spring.application.name:application}")
	private String serviceId;

	@Autowired
	private InetUtils inet;

	@Bean
	@ConditionalOnMissingBean
	public SimpleDiscoveryProperties simpleReactiveDiscoveryProperties() {
		SimpleDiscoveryProperties simple = new SimpleDiscoveryProperties();
		simple.getLocal().setServiceId(this.serviceId);
		simple.getLocal()
				.setUri(URI.create(
						"http://" + this.inet.findFirstNonLoopbackHostInfo().getHostname()
								+ ":" + findPort()));
		return simple;
	}

	@Bean
	@Order
	public SimpleReactiveDiscoveryClient simpleReactiveDiscoveryClient(
			SimpleDiscoveryProperties simpleDiscoveryProperties) {
		return new SimpleReactiveDiscoveryClient(simpleDiscoveryProperties);
	}

	private int findPort() {
		if (this.server != null && this.server.getPort() != null
				&& this.server.getPort() > 0) {
			return this.server.getPort();
		}
		return 8080;
	}

	@Bean
	@ConditionalOnProperty(
			value = "spring.cloud.discovery.client.health-indicator.enabled",
			matchIfMissing = true)
	public ReactiveDiscoveryHealthIndicator simpleReactiveDiscoveryClientHealthIndicator(
			SimpleReactiveDiscoveryClient discoveryClient,
			DiscoveryClientHealthIndicatorProperties properties) {
		return new ReactiveDiscoveryClientHealthIndicator(discoveryClient, properties);
	}

}
