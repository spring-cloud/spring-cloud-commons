package org.springframework.cloud.loadbalancer.core;

import java.util.List;

import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ServiceInstanceSupplier;

/**
 * @author Spencer Gibb
 */
public class CachingServiceInstanceSupplier implements ServiceInstanceSupplier {

	public static final String SERVICE_INSTANCE_CACHE_NAME = CachingServiceInstanceSupplier.class.getSimpleName()+"Cache";

	private final ServiceInstanceSupplier delegate;
	private final Flux<ServiceInstance> serviceInstances;

	@SuppressWarnings("unchecked")
	public CachingServiceInstanceSupplier(ServiceInstanceSupplier delegate, CacheManager cacheManager) {
		this.delegate = delegate;
		this.serviceInstances = CacheFlux.lookup(key -> {
			Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME); //TODO: configurable cache name
			List<ServiceInstance> list = cache.get(key, List.class);
			if (list == null || list.isEmpty()) {
				return Mono.empty();
			}
			return Flux.fromIterable(list)
					.materialize()
					.collectList();
		}, delegate.getServiceId())
				.onCacheMissResume(this.delegate::get)
				.andWriteWith((key, signals) -> Flux.fromIterable(signals)
						.dematerialize()
						.cast(ServiceInstance.class)
						.collectList()
						.doOnNext(instances -> {
							Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
							cache.put(key, instances);
						})
						.then());
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
