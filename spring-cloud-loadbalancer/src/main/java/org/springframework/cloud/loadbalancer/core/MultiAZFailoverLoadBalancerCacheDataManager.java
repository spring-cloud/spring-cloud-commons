/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.loadbalancer.core;

import java.util.HashSet;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;

import static org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier.SERVICE_INSTANCE_CACHE_NAME;

/**
 * A CacheDataManager implementation that logs connection-failed ServiceInstances.
 *
 * @author Jiwon Jeon
 */
public class MultiAZFailoverLoadBalancerCacheDataManager implements LoadBalancerCacheDataManager {

	private static final String CACHE_KEY_FAILED = "FAILED";

	private final LoadBalancerCacheManager cacheManager;

	public MultiAZFailoverLoadBalancerCacheDataManager(LoadBalancerCacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public Cache getCache() {
		return cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
	}

	@Override
	public void clear() {
		getCache().clear();
	}

	@Override
	public <T> T getInstance(String key, Class<T> classType) {
		return getCache().get(key, classType);
	}

	@Override
	public void putInstance(ServiceInstance instance) {
		Set<ServiceInstance> failedInstances = getInstance(CACHE_KEY_FAILED);

		if (failedInstances == null) {
			failedInstances = new HashSet<>();
		}

		failedInstances.add(instance);
		getCache().put(CACHE_KEY_FAILED, failedInstances);
	}

	public Set<ServiceInstance> getInstance(String key) {
		return getInstance(key, Set.class);
	}

}
