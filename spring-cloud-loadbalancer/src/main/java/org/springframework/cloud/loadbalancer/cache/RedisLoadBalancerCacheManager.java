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

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.Assert;

/**
 * Redis-based {@link LoadBalancerCacheManager} implementation for Spring Cloud
 * LoadBalancer.
 *
 * @author
 */
public class RedisLoadBalancerCacheManager extends AbstractCacheManager implements LoadBalancerCacheManager {

	private static final String SERVICE_INSTANCE_CACHE_NAME = "CachingServiceInstanceListSupplierCache";

	private final RedisConnectionFactory redisConnectionFactory;

	private final LoadBalancerCacheProperties cacheProperties;

	public RedisLoadBalancerCacheManager(RedisConnectionFactory redisConnectionFactory,
			LoadBalancerCacheProperties cacheProperties) {
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		Assert.notNull(cacheProperties, "LoadBalancerCacheProperties must not be null");
		this.redisConnectionFactory = redisConnectionFactory;
		this.cacheProperties = cacheProperties;
	}

	@Override
	protected Collection<? extends Cache> loadCaches() {
		return Collections.singletonList(createServiceInstanceCache());
	}

	private Cache createServiceInstanceCache() {
		return new RedisLoadBalancerCache(SERVICE_INSTANCE_CACHE_NAME, redisConnectionFactory,
				cacheProperties.getTtl());
	}

}
