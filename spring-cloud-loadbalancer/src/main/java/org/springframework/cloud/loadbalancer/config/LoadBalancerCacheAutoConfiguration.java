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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cloud.loadbalancer.cache.CaffeineBasedLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.EvictorBasedLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An AutoConfiguration that automatically enables caching when when Spring Boot, and
 * Spring Framework Cache support and Caffeine classes are present and warns if Caffeine
 * is not present (we are only warning about Caffeine because the other dependencies are
 * in spring-cloud-starter-loadbalancer).
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 * @see CacheManager
 * @see CacheAutoConfiguration
 * @see CacheAspectSupport
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ CacheManager.class, CacheAutoConfiguration.class })
@AutoConfigureAfter(CacheAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.cache.enabled",
		matchIfMissing = true)
@EnableConfigurationProperties(LoadBalancerCacheProperties.class)
public class LoadBalancerCacheAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("com.github.benmanes.caffeine.cache.Caffeine")
	protected static class LoadBalancerCacheManagerWarnConfiguration {

		@Bean
		LoadBalancerCaffeineWarnLogger caffeineWarnLogger() {
			return new LoadBalancerCaffeineWarnLogger();
		}

	}

	static class LoadBalancerCaffeineWarnLogger {

		private static final Log LOG = LogFactory
				.getLog(LoadBalancerCaffeineWarnLogger.class);

		@PostConstruct
		void logWarning() {
			if (LOG.isWarnEnabled()) {
				LOG.warn(
						"Spring Cloud LoadBalancer is currently working without cache. To enable cache, add "
								+ "com.github.ben-manes.caffeine:caffeine dependency to classpath.");
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Caffeine.class)
	protected static class CaffeineLoadBalancerCacheManagerConfiguration {

		@Bean(autowireCandidate = false)
		@ConditionalOnMissingBean
		LoadBalancerCacheManager caffeineLoadBalancerCacheManager(
				LoadBalancerCacheProperties cacheProperties) {
			return new CaffeineBasedLoadBalancerCacheManager(cacheProperties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("com.github.benmanes.caffeine.cache.Caffeine")
	@ConditionalOnClass(ConcurrentMapWithTimedEviction.class)
	protected static class EvictorLoadBalancerCacheManagerConfiguration {

		@Bean(autowireCandidate = false)
		@ConditionalOnMissingBean
		LoadBalancerCacheManager evictorLoadBalancerCacheManager(
				LoadBalancerCacheProperties cacheProperties) {
			return new EvictorBasedLoadBalancerCacheManager(cacheProperties);
		}

	}

}
