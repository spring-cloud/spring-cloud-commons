package org.springframework.cloud.loadbalancer.core;

import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.CacheSupplier;
import org.springframework.cloud.client.loadbalancer.reactive.ServiceInstanceSupplier;

/**
 * @author Spencer Gibb
 */
public class CachingServiceInstanceSupplier implements ServiceInstanceSupplier {

	private final ServiceInstanceSupplier delegate;
	private final Flux<ServiceInstance> serviceInstances;

	public CachingServiceInstanceSupplier(ServiceInstanceSupplier delegate, CacheSupplier cache) {
		this.delegate = delegate;
		this.serviceInstances = CacheFlux
				.lookup(cache.get(), delegate.getServiceId(), ServiceInstance.class)
				.onCacheMissResume(this.delegate::get);
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
