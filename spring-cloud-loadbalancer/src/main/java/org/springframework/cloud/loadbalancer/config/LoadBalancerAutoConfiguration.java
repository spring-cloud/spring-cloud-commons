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

package org.springframework.cloud.loadbalancer.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerDiscoveryClientConfiguration;
import org.springframework.cloud.loadbalancer.client.DefaultReactorLoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author Spencer Gibb
 */
@Configuration
@LoadBalancerClients
@AutoConfigureBefore({ ReactorLoadBalancerClientAutoConfiguration.class,
		ReactiveLoadBalancerAutoConfiguration.class })
// @EnableCaching //TODO: how to enforce, or check conditions?
// @AutoConfigureBefore(CacheAutoConfiguration.class)
@Import(LoadBalancerDiscoveryClientConfiguration.class)
public class LoadBalancerAutoConfiguration {

	private final ObjectProvider<List<LoadBalancerClientSpecification>> configurations;

	public LoadBalancerAutoConfiguration(
			ObjectProvider<List<LoadBalancerClientSpecification>> configurations) {
		this.configurations = configurations;
	}

	@Bean
	public LoadBalancerClientFactory loadBalancerClientFactory() {
		LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory();
		clientFactory.setConfigurations(
				this.configurations.getIfAvailable(Collections::emptyList));
		return clientFactory;
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactorLoadBalancerClient reactorLoadBalancerClient(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		return new DefaultReactorLoadBalancerClient(loadBalancerClientFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
			Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		return new RoundRobinLoadBalancer(name, loadBalancerClientFactory
				.getLazyProvider(name, ServiceInstanceSupplier.class));
	}

}
