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

package org.springframework.cloud.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.FeaturesEndpoint;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Cloud Commons Client.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@AutoConfigureOrder(0)
public class CommonsClientAutoConfiguration {

	@Configuration
	@EnableConfigurationProperties(DiscoveryClientHealthIndicatorProperties.class)
	@ConditionalOnClass(HealthIndicator.class)
	@ConditionalOnBean(DiscoveryClient.class)
	@ConditionalOnDiscoveryEnabled
	protected static class DiscoveryLoadBalancerConfiguration {

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.discovery.client.health-indicator.enabled", matchIfMissing = true)
		public DiscoveryClientHealthIndicator discoveryClientHealthIndicator(
				ObjectProvider<DiscoveryClient> discoveryClient,
				DiscoveryClientHealthIndicatorProperties properties) {
			return new DiscoveryClientHealthIndicator(discoveryClient, properties);
		}

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.discovery.client.composite-indicator.enabled", matchIfMissing = true)
		@ConditionalOnBean({ DiscoveryHealthIndicator.class, HealthAggregator.class })
		public DiscoveryCompositeHealthIndicator discoveryCompositeHealthIndicator(
				HealthAggregator aggregator, List<DiscoveryHealthIndicator> indicators) {
			return new DiscoveryCompositeHealthIndicator(aggregator, indicators);
		}

		@Bean
		public HasFeatures commonsFeatures() {
			return HasFeatures.abstractFeatures(DiscoveryClient.class,
					LoadBalancerClient.class);
		}

	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	@ConditionalOnProperty(value = "spring.cloud.features.enabled", matchIfMissing = true)
	protected static class ActuatorConfiguration {

		@Autowired(required = false)
		private List<HasFeatures> hasFeatures = new ArrayList<>();

		@Bean
		@ConditionalOnEnabledEndpoint
		public FeaturesEndpoint featuresEndpoint() {
			return new FeaturesEndpoint(this.hasFeatures);
		}

	}

}
