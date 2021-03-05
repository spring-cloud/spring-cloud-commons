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

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.reactive.OnNoRibbonDefaultCondition;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.blocking.retry.BlockingLoadBalancedRetryFactory;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * An autoconfiguration for {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.1.3
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
@AutoConfigureBefore({
		org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class,
		AsyncLoadBalancerAutoConfiguration.class })
public class BlockingLoadBalancerClientAutoConfiguration {

	@Bean
	@ConditionalOnClass(
			name = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.ribbon.enabled",
			matchIfMissing = true)
	public BlockingLoadBalancerClientRibbonWarnLogger blockingLoadBalancerClientRibbonWarnLogger() {
		return new BlockingLoadBalancerClientRibbonWarnLogger();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestTemplate.class)
	@Conditional(OnNoRibbonDefaultCondition.class)
	protected static class BlockingLoadbalancerClientConfig {

		@Bean
		@ConditionalOnBean(LoadBalancerClientFactory.class)
		@Primary
		public BlockingLoadBalancerClient blockingLoadBalancerClient(
				LoadBalancerClientFactory loadBalancerClientFactory) {
			return new BlockingLoadBalancerClient(loadBalancerClientFactory);
		}

		@Configuration
		@ConditionalOnClass(RetryTemplate.class)
		@EnableConfigurationProperties(LoadBalancerRetryProperties.class)
		protected static class BlockingLoadBalancerRetryConfig {

			@Bean
			@ConditionalOnMissingBean
			LoadBalancedRetryFactory loadBalancedRetryFactory(
					LoadBalancerRetryProperties retryProperties) {
				return new BlockingLoadBalancedRetryFactory(retryProperties);
			}

		}

	}

	static class BlockingLoadBalancerClientRibbonWarnLogger {

		private static final Log LOG = LogFactory
				.getLog(BlockingLoadBalancerClientRibbonWarnLogger.class);

		@PostConstruct
		void logWarning() {
			if (LOG.isWarnEnabled()) {
				LOG.warn(
						"You already have RibbonLoadBalancerClient on your classpath. It will be used by default. "
								+ "As Spring Cloud Ribbon is in maintenance mode. We recommend switching to "
								+ BlockingLoadBalancerClient.class.getSimpleName()
								+ " instead. In order to use it, set the value of `spring.cloud.loadbalancer.ribbon.enabled` to `false` or "
								+ "remove spring-cloud-starter-netflix-ribbon from your project.");
			}
		}

	}

}
