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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An auto-configuration that provides a {@link BeanPostProcessor} that allows the use of
 * a {@link LoadBalanced} {@link WebClient.Builder} with
 * {@link ReactorLoadBalancerExchangeFilterFunction} and {@link ReactiveLoadBalancer} used
 * under the hood. NOTE: This has been extracted to a separate configuration in order to
 * not impact instantiation and post-processing of other Reactor-LoadBalancer-related
 * beans.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@Conditional(LoadBalancerBeanPostProcessorAutoConfiguration.OnAnyLoadBalancerImplementationPresentCondition.class)
public class LoadBalancerBeanPostProcessorAutoConfiguration {

	@Bean
	public LoadBalancerWebClientBuilderBeanPostProcessor loadBalancerWebClientBuilderBeanPostProcessor(
			DeferringLoadBalancerExchangeFilterFunction deferringExchangeFilterFunction, ApplicationContext context) {
		return new LoadBalancerWebClientBuilderBeanPostProcessor(deferringExchangeFilterFunction, context);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ReactiveLoadBalancer.Factory.class)
	protected static class ReactorDeferringLoadBalancerFilterConfig {

		@Bean
		@Primary
		DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction> reactorDeferringLoadBalancerExchangeFilterFunction(
				ObjectProvider<LoadBalancedExchangeFilterFunction> exchangeFilterFunctionProvider) {
			return new DeferringLoadBalancerExchangeFilterFunction<>(exchangeFilterFunctionProvider);
		}

	}

	static final class OnAnyLoadBalancerImplementationPresentCondition extends AnyNestedCondition {

		private OnAnyLoadBalancerImplementationPresentCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(ReactiveLoadBalancer.Factory.class)
		static class ReactiveLoadBalancerFactoryPresent {

		}

		@ConditionalOnBean(LoadBalancerClient.class)
		static class LoadBalancerClientPresent {

		}

	}

}
