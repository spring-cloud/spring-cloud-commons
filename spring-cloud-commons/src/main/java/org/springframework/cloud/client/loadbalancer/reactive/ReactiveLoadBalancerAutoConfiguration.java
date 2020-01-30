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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.endpoint.LoadBalancerEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(ReactorLoadBalancerClientAutoConfiguration.class)
public class ReactiveLoadBalancerAutoConfiguration {

	@Bean
	@ConditionalOnClass(
			name = "org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnMissingBean(ReactorLoadBalancerExchangeFilterFunction.class)
	@ConditionalOnBean(LoadBalancerClient.class)
	@Deprecated
	public LoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(
			LoadBalancerClient client) {
		return new LoadBalancerExchangeFilterFunction(client);
	}

	@Bean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnClass(
			name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
	@ConditionalOnBean(ReactiveLoadBalancer.Factory.class)
	public LoadBalancerEndpoint loadBalancerEndpoint(
			ObjectProvider<ReactiveLoadBalancer.Factory<ServiceInstance>> clientFactory) {
		return new LoadBalancerEndpoint(clientFactory);
	}

}
