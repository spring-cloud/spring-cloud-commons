/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.blocking.XForwardedHeadersTransformer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.blocking.retry.BlockingLoadBalancedRetryFactory;
import org.springframework.cloud.loadbalancer.core.LoadBalancerServiceInstanceCookieTransformer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * An autoconfiguration for {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Gandhimathi Velusamy
 * @since 2.1.3
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
@AutoConfigureBefore({ org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class,
		AsyncLoadBalancerAutoConfiguration.class })
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
public class BlockingLoadBalancerClientAutoConfiguration {

	@Bean
	@ConditionalOnBean(LoadBalancerClientFactory.class)
	@ConditionalOnMissingBean
	public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		return new BlockingLoadBalancerClient(loadBalancerClientFactory);
	}

	@Bean
	@ConditionalOnBean(LoadBalancerClientFactory.class)
	@ConditionalOnMissingBean(LoadBalancerServiceInstanceCookieTransformer.class)
	public LoadBalancerServiceInstanceCookieTransformer loadBalancerServiceInstanceCookieTransformer(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		return new LoadBalancerServiceInstanceCookieTransformer(loadBalancerClientFactory);
	}

	@Bean
	@ConditionalOnMissingBean(XForwardedHeadersTransformer.class)
	@ConditionalOnBean(LoadBalancerClientFactory.class)
	public XForwardedHeadersTransformer xForwarderHeadersTransformer(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		return new XForwardedHeadersTransformer(loadBalancerClientFactory);
	}

	@Configuration
	@ConditionalOnClass(RetryTemplate.class)
	@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
	protected static class BlockingLoadBalancerRetryConfig {

		@Bean
		@ConditionalOnMissingBean
		LoadBalancedRetryFactory loadBalancedRetryFactory(
				ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
			return new BlockingLoadBalancedRetryFactory(loadBalancerFactory);
		}

	}

}
