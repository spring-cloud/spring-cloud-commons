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

package org.springframework.cloud.loadbalancer.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerBeanPostProcessorAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients
@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
@AutoConfigureBefore({ ReactorLoadBalancerClientAutoConfiguration.class,
		LoadBalancerBeanPostProcessorAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
public class LoadBalancerAutoConfiguration {

	private final ObjectProvider<List<LoadBalancerClientSpecification>> configurations;

	public LoadBalancerAutoConfiguration(ObjectProvider<List<LoadBalancerClientSpecification>> configurations) {
		this.configurations = configurations;
	}

	@Bean
	@ConditionalOnMissingBean
	public LoadBalancerZoneConfig zoneConfig(Environment environment) {
		return new LoadBalancerZoneConfig(environment.getProperty("spring.cloud.loadbalancer.zone"));
	}

	@ConditionalOnMissingBean
	@Bean
	public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties) {
		LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory(properties);
		clientFactory.setConfigurations(this.configurations.getIfAvailable(Collections::emptyList));
		return clientFactory;
	}

}
