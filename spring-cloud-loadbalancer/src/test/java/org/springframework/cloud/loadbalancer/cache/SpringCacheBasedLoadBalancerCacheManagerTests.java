/*
 * Copyright 2012-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringCacheBasedLoadBalancerCacheManagerTests {

	@Test
	void shouldDelegateGetCache() {
		ConcurrentMapCacheManager delegate = new ConcurrentMapCacheManager("testCache");
		SpringCacheBasedLoadBalancerCacheManager cacheManager = new SpringCacheBasedLoadBalancerCacheManager(delegate);

		assertThat(cacheManager.getCache("testCache")).isSameAs(delegate.getCache("testCache"));
	}

	@Test
	void shouldDelegateGetCacheNames() {
		ConcurrentMapCacheManager delegate = new ConcurrentMapCacheManager("testCache");
		SpringCacheBasedLoadBalancerCacheManager cacheManager = new SpringCacheBasedLoadBalancerCacheManager(delegate);

		assertThat(cacheManager.getCacheNames()).containsExactly("testCache");
	}

	@Test
	void shouldReturnNullWhenCacheDoesNotExist() {
		ConcurrentMapCacheManager delegate = new ConcurrentMapCacheManager("testCache");
		SpringCacheBasedLoadBalancerCacheManager cacheManager = new SpringCacheBasedLoadBalancerCacheManager(delegate);

		Cache cache = cacheManager.getCache("missingCache");

		assertThat(cache).isNull();
	}

	@Test
	void shouldRejectNullCacheManagerDelegate() {
		assertThatThrownBy(() -> new SpringCacheBasedLoadBalancerCacheManager(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("CacheManager delegate must not be null");
	}

}
