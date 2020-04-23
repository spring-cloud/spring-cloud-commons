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

package org.springframework.cloud.loadbalancer.cache;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier.SERVICE_INSTANCE_CACHE_NAME;

/**
 * Tests for {@link DefaultLoadBalancerCacheManager}.
 *
 * @author Olga Maciaszek-Sharma
 */
class DefaultLoadBalancerCacheManagerTests {

	@SuppressWarnings("ConstantConditions")
	@Test
	void shouldCreateLoadBalancerCacheFromProperties() {
		LoadBalancerCacheProperties properties = new LoadBalancerCacheProperties();
		properties.setTtl(Duration.ofMinutes(5));
		properties.setCapacity(128);

		DefaultLoadBalancerCacheManager cacheManager = new DefaultLoadBalancerCacheManager(
				properties);

		assertThat(cacheManager.getCacheNames()).hasSize(1);
		assertThat(cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME))
				.isInstanceOf(DefaultLoadBalancerCache.class);
		assertThat(((DefaultLoadBalancerCache) cacheManager
				.getCache(SERVICE_INSTANCE_CACHE_NAME)).getEvictMs()).isEqualTo(300000);
	}

	@Test
	void shouldNotThrowExceptionOnDuplicateCacheName() {
		LoadBalancerCacheProperties properties = new LoadBalancerCacheProperties();

		assertThatCode(
				() -> new DefaultLoadBalancerCacheManager(properties, "test", "test"))
						.doesNotThrowAnyException();
	}

	@Test
	void shouldOnlyCreateOneCacheWithGivenName() {
		LoadBalancerCacheProperties properties = new LoadBalancerCacheProperties();

		CacheManager cacheManager = new DefaultLoadBalancerCacheManager(properties,
				"test", "test");

		assertThat(cacheManager.getCacheNames()).hasSize(1);
		assertThat(cacheManager.getCache("test"))
				.isInstanceOf(DefaultLoadBalancerCache.class);
	}

}
