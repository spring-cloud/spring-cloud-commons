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

package org.springframework.cloud.loadbalancer.config;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.nonreactive.client.SpringLoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@LoadBalancerClients
@AutoConfigureAfter({ AsyncLoadBalancerAutoConfiguration.class,
		LoadBalancerAutoConfiguration.class })
@AutoConfigureBefore({
		org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class })
@ConditionalOnBean(LoadBalancerClientFactory.class)
public class SpringLoadBalancerClientAutoConfiguration {

	@Bean
	@ConditionalOnClass(
			name = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
	public RibbonWarnLogger ribbonWarnLogger() {
		return new RibbonWarnLogger();
	}

	@Bean
	@ConditionalOnClass(RestTemplate.class)
	@ConditionalOnMissingBean
	@ConditionalOnMissingClass("org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
	public LoadBalancerClient loadBalancerClient(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		return new SpringLoadBalancerClient(loadBalancerClientFactory);
	}

}

class RibbonWarnLogger {

	private static final Log LOG = LogFactory.getLog(RibbonWarnLogger.class);

	@PostConstruct
	void logWarning() {
		if (LOG.isWarnEnabled()) {
			LOG.warn(
					"You already have RibbonLoadBalancerClient on your classpath. It will be used by default."
							+ " To use SpringLoadBalancerClient remove spring-cloud-starter-netflix-ribbon from your project.");
		}
	}

}
