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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An auto-configuration that allows the use of a {@link LoadBalanced}
 * {@link WebClient.Builder} with {@link ReactorLoadBalancerExchangeFilterFunction} and
 * {@link ReactiveLoadBalancer} used under the hood.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.1.3
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@ConditionalOnBean(ReactiveLoadBalancer.Factory.class)
public class ReactorLoadBalancerClientAutoConfiguration {

	private List<WebClient.Builder> webClientBuilders = Collections.emptyList();

	List<WebClient.Builder> getBuilders() {
		return this.webClientBuilders;
	}

	@Bean
	public SmartInitializingSingleton loadBalancedWebClientInitializer(
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
	public WebClientCustomizer loadBalancerClientWebClientCustomizer(
			ReactorLoadBalancerExchangeFilterFunction filterFunction) {
		return builder -> builder.filter(filterFunction);
	}

	@Bean
	public ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(
			ReactiveLoadBalancer.Factory loadBalancerFactory) {
		return new ReactorLoadBalancerExchangeFilterFunction(loadBalancerFactory);
	}

	@LoadBalanced
	@Autowired(required = false)
	void setWebClientBuilders(List<WebClient.Builder> webClientBuilders) {
		this.webClientBuilders = webClientBuilders;
	}

}
