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

package org.springframework.cloud.loadbalancer.cache;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier.SERVICE_INSTANCE_CACHE_NAME;

/**
 * A Spring Cloud LoadBalancer specifi implementation of {@link CaffeineCacheManager} that implements the {@link LoadBalancerCacheManager} marker interface.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class CaffeineBasedLoadBalancerCacheManager extends CaffeineCacheManager
		implements LoadBalancerCacheManager {

	public CaffeineBasedLoadBalancerCacheManager(String specification) {
		super(SERVICE_INSTANCE_CACHE_NAME);
		if (!StringUtils.isEmpty(specification)) {
			setCacheSpecification(specification);
		}
		else {
			setCaffeine(Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
					.softValues());
		}
	}

}
