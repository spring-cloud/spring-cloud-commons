/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer.impl.annotation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.impl.scoped.CachedDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.impl.scoped.RoundRobinScopedLoadBalancer;
import org.springframework.cloud.client.loadbalancer.impl.scoped.ScopedLoadBalancer;
import org.springframework.cloud.client.loadbalancer.impl.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


/**
 * @author Dave Syer
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableConfigurationProperties
public class LoadBalancerClientConfiguration {

	@Value("${loadbalancer.client.name}")
	private String name = "client";

	@Bean
	@ConditionalOnMissingBean
	public CachedDiscoveryClient cachingDiscoveryClient(DiscoveryClient discoveryClient, Environment env) {
		return new CachedDiscoveryClient(discoveryClient, env);
	}

	@Bean
	@ConditionalOnMissingBean
	public ScopedLoadBalancer algorithm(LoadBalancerClientFactory clientFactory) {
		return new RoundRobinScopedLoadBalancer(clientFactory);
	}

}
