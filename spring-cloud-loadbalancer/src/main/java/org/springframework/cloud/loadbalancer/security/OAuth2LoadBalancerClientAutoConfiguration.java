/*
 * Copyright 2015-2015 the original author or authors.
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

package org.springframework.cloud.loadbalancer.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

/**
 * @author Dave Syer
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OAuth2RestTemplate.class)
@ConditionalOnProperty("spring.cloud.oauth2.load-balanced.enabled")
@AutoConfigureAfter(OAuth2AutoConfiguration.class)
@Deprecated // spring-security-oauth2 reached EOL
public class OAuth2LoadBalancerClientAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(LoadBalancerInterceptor.class)
	protected static class UserInfoLoadBalancerConfig {

		@Bean
		public UserInfoRestTemplateCustomizer loadBalancedUserInfoRestTemplateCustomizer(
				final LoadBalancerInterceptor loadBalancerInterceptor) {
			return restTemplate -> {
				List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
				interceptors.add(loadBalancerInterceptor);
				restTemplate.setInterceptors(interceptors);
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(RetryLoadBalancerInterceptor.class)
	protected static class UserInfoRetryLoadBalancerConfig {

		@Bean
		public UserInfoRestTemplateCustomizer retryLoadBalancedUserInfoRestTemplateCustomizer(
				final RetryLoadBalancerInterceptor loadBalancerInterceptor) {
			return new UserInfoRestTemplateCustomizer() {
				@Override
				public void customize(OAuth2RestTemplate restTemplate) {
					List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
					interceptors.add(loadBalancerInterceptor);
					restTemplate.setInterceptors(interceptors);
				}
			};
		}

	}

}
