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

import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.ServiceInstance;

/**
 * @deprecated Use {@link CachingServiceInstanceListSupplier} instead.
 * @author Spencer Gibb
 */
@Deprecated
public class CachingServiceInstanceSupplier implements ServiceInstanceSupplier {

	/**
	 * Name of the service cache instance.
	 */
	public static final String SERVICE_INSTANCE_CACHE_NAME = CachingServiceInstanceSupplier.class
			.getSimpleName() + "Cache";

	private final ServiceInstanceSupplier delegate;

	private final Flux<ServiceInstance> serviceInstances;

	@SuppressWarnings("unchecked")
	public CachingServiceInstanceSupplier(ServiceInstanceSupplier delegate,
			CacheManager cacheManager) {
		this.delegate = delegate;
		this.serviceInstances = CacheFlux.lookup(key -> {
			// TODO: configurable cache name
			Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
			List<ServiceInstance> list = cache.get(key, List.class);
			if (list == null || list.isEmpty()) {
				return Mono.empty();
			}
			return Flux.fromIterable(list).materialize().collectList();
		}, delegate.getServiceId()).onCacheMissResume(this.delegate::get)
				.andWriteWith((key, signals) -> Flux.fromIterable(signals).dematerialize()
						.cast(ServiceInstance.class).collectList().doOnNext(instances -> {
							Cache cache = cacheManager
									.getCache(SERVICE_INSTANCE_CACHE_NAME);
							cache.put(key, instances);
						}).then());
	}

	@Override
	public Flux<ServiceInstance> get() {
		return this.serviceInstances;
	}

	@Override
	public String getServiceId() {
		return this.delegate.getServiceId();
	}

}
