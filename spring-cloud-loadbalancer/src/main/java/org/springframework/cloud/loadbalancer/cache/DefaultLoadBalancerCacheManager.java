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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.scheduler.DelayedTaskEvictionScheduler;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import static org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier.SERVICE_INSTANCE_CACHE_NAME;

/**
 * An {@link DefaultLoadBalancerCache}-based {@link LoadBalancerCacheManager}
 * implementation.
 *
 * NOTE: This is a very basic implementation as required for the LoadBalancer caching
 * mechanism at the moment. The underlying implementation can be modified in future to
 * allow for passing different properties per cache name.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 * @see <a href="https://github.com/stoyanr/Evictor">Evictor</a>
 * @see ConcurrentHashMapWithTimedEviction
 */
public class DefaultLoadBalancerCacheManager implements LoadBalancerCacheManager {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	public DefaultLoadBalancerCacheManager(LoadBalancerCacheProperties loadBalancerCacheProperties,
			String... cacheNames) {
		cacheMap.putAll(createCaches(cacheNames, loadBalancerCacheProperties).stream()
				.collect(Collectors.toMap(DefaultLoadBalancerCache::getName, cache -> cache)));
	}

	public DefaultLoadBalancerCacheManager(LoadBalancerCacheProperties loadBalancerCacheProperties) {
		this(loadBalancerCacheProperties, SERVICE_INSTANCE_CACHE_NAME);
	}

	private Set<DefaultLoadBalancerCache> createCaches(String[] cacheNames,
			LoadBalancerCacheProperties loadBalancerCacheProperties) {
		return Arrays.stream(cacheNames).distinct()
				.map(name -> new DefaultLoadBalancerCache(name,
						new ConcurrentHashMapWithTimedEviction<>(loadBalancerCacheProperties.getCapacity(),
								new DelayedTaskEvictionScheduler<>(aScheduledDaemonThreadExecutor())),
						loadBalancerCacheProperties.getTtl().toMillis(), false))
				.collect(Collectors.toSet());
	}

	private ScheduledExecutorService aScheduledDaemonThreadExecutor() {
		return Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			thread.setDaemon(true);
			return thread;
		});
	}

	@Override
	@Nullable
	public Cache getCache(String name) {
		return cacheMap.get(name);
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(cacheMap.keySet());
	}

}
