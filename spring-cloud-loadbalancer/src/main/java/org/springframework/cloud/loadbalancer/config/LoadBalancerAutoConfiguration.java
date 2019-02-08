/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.loadbalancer.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.WebClientCustomizer;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactiveLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Spencer Gibb
 * @author Nikola Kološnjaji
 */
@Configuration
@LoadBalancerClients
// @EnableCaching //TODO: how to enforce, or check conditions?
// @AutoConfigureBefore(CacheAutoConfiguration.class)
public class LoadBalancerAutoConfiguration {

	@LoadBalanced
	@Autowired(required = false)
	private List<WebClient.Builder> webClientBuilders = Collections.emptyList();

	public List<WebClient.Builder> getBuilders() {
		return webClientBuilders;
	}

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
	public SmartInitializingSingleton reactiveLoadBalancedWebClientInitializer(
			final List<WebClientCustomizer> customizers) {
		return () -> {
			for (WebClient.Builder webClientBuilder : getBuilders()) {
				for (WebClientCustomizer customizer : customizers) {
					customizer.customize(webClientBuilder);
				}
			}
		};
	}

	@Bean
	public WebClientCustomizer reactiveLoadbalanceClientWebClientCustomizer(
			ReactiveLoadBalancerExchangeFilterFunction filterFunction) {
		return builder -> builder.filter(filterFunction);
	}

	@Bean
	public ReactiveLoadBalancerExchangeFilterFunction reactiveLoadBalancerExchangeFilterFunction() {
		return new ReactiveLoadBalancerExchangeFilterFunction(
				loadBalancerClientFactory());
	}

}
