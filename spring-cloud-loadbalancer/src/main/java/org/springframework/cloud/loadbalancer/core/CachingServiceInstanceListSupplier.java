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

package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
public class CachingServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final Log log = LogFactory.getLog(CachingServiceInstanceListSupplier.class);

	/**
	 * Name of the service cache instance.
	 */
	public static final String SERVICE_INSTANCE_CACHE_NAME = CachingServiceInstanceListSupplier.class.getSimpleName()
			+ "Cache";

	private final Flux<List<ServiceInstance>> serviceInstances;

	@SuppressWarnings("unchecked")
	public CachingServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, CacheManager cacheManager) {
		super(delegate);
		String serviceId = delegate.getServiceId();
		this.serviceInstances = Flux.defer(() -> Mono.fromCallable(() -> {
			// TODO: configurable cache name
			Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
			if (cache == null) {
				if (log.isErrorEnabled()) {
					log.error("Unable to find cache: " + SERVICE_INSTANCE_CACHE_NAME);
				}
				return Optional.<List<ServiceInstance>>empty();
			}
			return Optional.ofNullable((List<ServiceInstance>) cache.get(serviceId, List.class));
		}).subscribeOn(Schedulers.boundedElastic()).flatMapMany(cachedInstances -> {
			if (cachedInstances.isPresent() && !cachedInstances.get().isEmpty()) {
				return Flux.just(cachedInstances.get());
			}
			return delegate.get().take(1).flatMap(instances -> Mono.fromRunnable(() -> {
				Cache writeCache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
				if (writeCache == null) {
					if (log.isErrorEnabled()) {
						log.error("Unable to find cache for writing: " + SERVICE_INSTANCE_CACHE_NAME);
					}
				}
				else {
					writeCache.put(serviceId, instances);
				}
			}).subscribeOn(Schedulers.boundedElastic()).thenReturn(instances));
		}));
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return serviceInstances;
	}

}
