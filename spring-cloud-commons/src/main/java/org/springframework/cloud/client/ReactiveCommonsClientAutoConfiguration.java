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

package org.springframework.cloud.client;

import java.util.Collection;

import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryCompositeHealthContributor;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryHealthIndicator;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive Spring Cloud Commons
 * Client.
 *
 * @author Tim Ysewyn
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
public class ReactiveCommonsClientAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DiscoveryClientHealthIndicatorProperties.class)
	@ConditionalOnClass(ReactiveHealthIndicator.class)
	@ConditionalOnBean(ReactiveDiscoveryClient.class)
	@ConditionalOnDiscoveryEnabled
	@ConditionalOnReactiveDiscoveryEnabled
	protected static class ReactiveDiscoveryLoadBalancerConfiguration {

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.discovery.client.composite-indicator.enabled",
				matchIfMissing = true)
		@ConditionalOnBean({ ReactiveDiscoveryHealthIndicator.class })
		public ReactiveDiscoveryCompositeHealthContributor reactiveDiscoveryClients(
				Collection<ReactiveDiscoveryHealthIndicator> indicators) {
			return new ReactiveDiscoveryCompositeHealthContributor(indicators);
		}

		@Bean
		public HasFeatures reactiveCommonsFeatures() {
			return HasFeatures.abstractFeatures(ReactiveDiscoveryClient.class, ReactiveLoadBalancer.class);
		}

	}

}
