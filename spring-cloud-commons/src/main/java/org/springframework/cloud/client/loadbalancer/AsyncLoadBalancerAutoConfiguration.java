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

package org.springframework.cloud.client.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.web.client.AsyncRestTemplate;

/**
 * Auto-configuration for Ribbon (client-side load balancing).
 *
 * @author Rob Worsnop
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(LoadBalancerClient.class)
@ConditionalOnClass(AsyncRestTemplate.class)
public class AsyncLoadBalancerAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class AsyncRestTemplateCustomizerConfig {

		@LoadBalanced
		@Autowired(required = false)
		private List<AsyncRestTemplate> restTemplates = Collections.emptyList();

		@Bean
		public SmartInitializingSingleton loadBalancedAsyncRestTemplateInitializer(
				final List<AsyncRestTemplateCustomizer> customizers) {
			return new SmartInitializingSingleton() {
				@Override
				public void afterSingletonsInstantiated() {
					for (AsyncRestTemplate restTemplate : AsyncRestTemplateCustomizerConfig.this.restTemplates) {
						for (AsyncRestTemplateCustomizer customizer : customizers) {
							customizer.customize(restTemplate);
						}
					}
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LoadBalancerInterceptorConfig {

		@Bean
		public AsyncLoadBalancerInterceptor asyncLoadBalancerInterceptor(
				LoadBalancerClient loadBalancerClient) {
			return new AsyncLoadBalancerInterceptor(loadBalancerClient);
		}

		@Bean
		public AsyncRestTemplateCustomizer asyncRestTemplateCustomizer(
				final AsyncLoadBalancerInterceptor loadBalancerInterceptor) {
			return new AsyncRestTemplateCustomizer() {
				@Override
				public void customize(AsyncRestTemplate restTemplate) {
					List<AsyncClientHttpRequestInterceptor> list = new ArrayList<>(
							restTemplate.getInterceptors());
					list.add(loadBalancerInterceptor);
					restTemplate.setInterceptors(list);
				}
			};
		}

	}

}
