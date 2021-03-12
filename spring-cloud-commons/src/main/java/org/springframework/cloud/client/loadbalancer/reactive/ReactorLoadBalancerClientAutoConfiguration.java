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

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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

	@Bean
	@ConditionalOnClass(
			name = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.ribbon.enabled",
			matchIfMissing = true)
	public ReactorLoadBalancerClientRibbonWarnLogger reactorLoadBalancerClientRibbonWarnLogger() {
		return new ReactorLoadBalancerClientRibbonWarnLogger();
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnNoRibbonDefaultCondition.class)
	protected static class ReactorLoadBalancerExchangeFilterFunctionConfig {

		@ConditionalOnMissingBean
		@Bean
		public ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(
				ReactiveLoadBalancer.Factory loadBalancerFactory,
				ObjectProvider<List<LoadBalancerClientRequestTransformer>> transformers) {
			return new ReactorLoadBalancerExchangeFilterFunction(loadBalancerFactory,
					transformers.getIfAvailable(Collections::emptyList));
		}

	}

	private static class ReactorLoadBalancerClientRibbonWarnLogger {

		private static final Log LOG = LogFactory
				.getLog(ReactorLoadBalancerClientRibbonWarnLogger.class);

		@PostConstruct
		void logWarning() {
			if (LOG.isWarnEnabled()) {
				LOG.warn("You have RibbonLoadBalancerClient on your classpath. "
						+ "LoadBalancerExchangeFilterFunction that uses it under the "
						+ "hood will be used by default. Spring Cloud Ribbon is now in maintenance mode, "
						+ "so we suggest switching to "
						+ ReactorLoadBalancerExchangeFilterFunction.class.getSimpleName()
						+ " instead. In order to use it, set the value of `spring.cloud.loadbalancer.ribbon.enabled` to `false` or "
						+ "remove spring-cloud-starter-netflix-ribbon from your project.");
			}
		}

	}

}
