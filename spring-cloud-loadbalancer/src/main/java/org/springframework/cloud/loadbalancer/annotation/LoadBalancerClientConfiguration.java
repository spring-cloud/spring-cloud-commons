/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.loadbalancer.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
public class LoadBalancerClientConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DiscoveryClientServiceInstanceSupplier discoveryClientServiceInstanceSupplier(
			DiscoveryClient discoveryClient, Environment env, CacheManager cacheManager) {
		return new DiscoveryClientServiceInstanceSupplier(discoveryClient, env,
				cacheManager);
	}

}
