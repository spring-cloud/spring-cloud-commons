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

package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An auto-configuration that allows the use of a {@link LoadBalanced}
 * {@link WebClient.Builder} with {@link ReactorLoadBalancerExchangeFilterFunction} and
 * {@link ReactiveLoadBalancer} used under the hood.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@ConditionalOnBean(ReactiveLoadBalancer.Factory.class)
public class ReactorLoadBalancerClientAutoConfiguration {

	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "false",
			matchIfMissing = true)
	@Bean
	public ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory, LoadBalancerProperties properties) {
		return new ReactorLoadBalancerExchangeFilterFunction(loadBalancerFactory, properties);
	}

	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true")
	@Bean
	public RetryableLoadBalancerExchangeFilterFunction retryableLoadBalancerExchangeFilterFunction(
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory, LoadBalancerProperties properties,
			LoadBalancerRetryPolicy retryPolicy) {
		return new RetryableLoadBalancerExchangeFilterFunction(retryPolicy, loadBalancerFactory, properties);
	}

	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true")
	@Bean
	public LoadBalancerRetryPolicy loadBalancerRetryPolicy(LoadBalancerProperties properties) {
		return new RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(properties);
	}

}
