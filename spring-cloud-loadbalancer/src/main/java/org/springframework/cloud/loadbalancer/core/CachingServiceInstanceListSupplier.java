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

package org.springframework.cloud.loadbalancer.core;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.ServiceInstance;

/**
 * A {@link ServiceInstanceListSupplier} implementation that tries retrieving
 * {@link ServiceInstance} objects from cache; if none found, retrieves instances using
 * {@link DiscoveryClientServiceInstanceListSupplier}.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class CachingServiceInstanceListSupplier
		extends DelegatingServiceInstanceListSupplier {

	private static final Log log = LogFactory
			.getLog(CachingServiceInstanceListSupplier.class);

	/**
	 * Name of the service cache instance.
	 */
	public static final String SERVICE_INSTANCE_CACHE_NAME = CachingServiceInstanceListSupplier.class
			.getSimpleName() + "Cache";

	private final Flux<List<ServiceInstance>> serviceInstances;

	@SuppressWarnings("unchecked")
	public CachingServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			CacheManager cacheManager) {
		super(delegate);
		this.serviceInstances = CacheFlux.lookup(key -> {
			// TODO: configurable cache name
			Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
			if (cache == null) {
				if (log.isErrorEnabled()) {
					log.error("Unable to find cache: " + SERVICE_INSTANCE_CACHE_NAME);
				}
				return Mono.empty();
			}
			List<ServiceInstance> list = cache.get(key, List.class);
			if (list == null || list.isEmpty()) {
				return Mono.empty();
			}
			return Flux.just(list).materialize().collectList();
		}, delegate.getServiceId()).onCacheMissResume(delegate.get().take(1))
				.andWriteWith((key, signals) -> Flux.fromIterable(signals).dematerialize()
						.doOnNext(instances -> {
							Cache cache = cacheManager
									.getCache(SERVICE_INSTANCE_CACHE_NAME);
							if (cache == null) {
								if (log.isErrorEnabled()) {
									log.error("Unable to find cache for writing: "
											+ SERVICE_INSTANCE_CACHE_NAME);
								}
							}
							else {
								cache.put(key, instances);
							}
						}).then());
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return serviceInstances;
	}

}
