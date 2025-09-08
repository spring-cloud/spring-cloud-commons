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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier.SERVICE_INSTANCE_CACHE_NAME;

/**
 * @author wind57
 */
class CaffeineBasedLoadBalancerCacheManagerTests {

	private static final String SERVICE_ID = "mock-service-id";

	private final CaffeineBasedLoadBalancerCacheManager caffeineBasedLoadBalancerCacheManager = new CaffeineBasedLoadBalancerCacheManager(
			SERVICE_INSTANCE_CACHE_NAME, getLoadBalancerCacheProperties());

	/**
	 * <pre>
	 *     - cache has a TTL of 3 seconds
	 *     - we issue a GET and its not in the cache (counter is incremented)
	 *     - we issue a second GET and it's in the cache this time (counter stays the same)
	 * </pre>
	 */
	@Test
	void testSecondGetRetrievedFromCache() {

		ServiceInstanceListSupplier serviceInstanceListSupplier = new StubServiceInstanceListSupplier();

		CachingServiceInstanceListSupplier cachingServiceInstanceListSupplier = new CachingServiceInstanceListSupplier(
				serviceInstanceListSupplier, caffeineBasedLoadBalancerCacheManager);

		List<ServiceInstance> serviceInstances = cachingServiceInstanceListSupplier.get().blockFirst();
		assertThat(serviceInstances).hasSize(1);
		// the first time we retrieve, the entry is not in the cache
		assertThat(StubServiceInstanceListSupplier.counter.get()).isEqualTo(1);

		List<ServiceInstance> serviceInstancesInTheCache = cachingServiceInstanceListSupplier.get().blockFirst();
		assertThat(serviceInstancesInTheCache).hasSize(1);
		// the second time we retrieve, the entry is in the cache
		// the underlying list supplier is not called
		assertThat(StubServiceInstanceListSupplier.counter.get()).isEqualTo(1);

	}

	/**
	 * <pre>
	 *     - cache has a TTL of 3 seconds
	 *     - we issue a GET and its not in the cache (counter is incremented)
	 *     - we issue a second GET and it's in the cache this time (counter stays the same)
	 *     - we wait for 4 seconds (entry in the cache is evicted)
	 *     - we issue one more GET and this time its not in the cache (counter is incremented to 2)
	 * </pre>
	 */
	@Test
	void testSecondGetNoInTheCache() throws Exception {

		ServiceInstanceListSupplier serviceInstanceListSupplier = new StubServiceInstanceListSupplier();

		CachingServiceInstanceListSupplier cachingServiceInstanceListSupplier = new CachingServiceInstanceListSupplier(
				serviceInstanceListSupplier, caffeineBasedLoadBalancerCacheManager);

		List<ServiceInstance> serviceInstances = cachingServiceInstanceListSupplier.get().blockFirst();
		assertThat(serviceInstances).hasSize(1);
		// the first time we retrieve, the entry is not in the cache
		assertThat(StubServiceInstanceListSupplier.counter.get()).isEqualTo(1);

		List<ServiceInstance> serviceInstancesInTheCache = cachingServiceInstanceListSupplier.get().blockFirst();
		assertThat(serviceInstancesInTheCache).hasSize(1);
		// the second time we retrieve, the entry is in the cache,
		// the underlying list supplier is not called
		assertThat(StubServiceInstanceListSupplier.counter.get()).isEqualTo(1);

		Thread.sleep(4_000);

		List<ServiceInstance> serviceInstancesNotInTheCache = cachingServiceInstanceListSupplier.get().blockFirst();
		assertThat(serviceInstancesNotInTheCache).hasSize(1);
		// the third time we retrieve, the entry is not in the cache,
		// the underlying list supplier is called
		assertThat(StubServiceInstanceListSupplier.counter.get()).isEqualTo(2);

	}

	@AfterEach
	void afterEach() {
		StubServiceInstanceListSupplier.counter.set(0);
	}

	private static LoadBalancerCacheProperties getLoadBalancerCacheProperties() {
		LoadBalancerCacheProperties properties = new LoadBalancerCacheProperties();
		properties.setCapacity(100);
		properties.setTtl(Duration.ofSeconds(3));
		return properties;
	}

	static class StubServiceInstanceListSupplier implements ServiceInstanceListSupplier {

		static AtomicInteger counter = new AtomicInteger(0);

		@Override
		public String getServiceId() {
			return SERVICE_ID;
		}

		@Override
		public Flux<List<ServiceInstance>> get() {
			List<ServiceInstance> serviceInstances = List
				.of(new DefaultServiceInstance(SERVICE_ID, SERVICE_ID, "localhost", 80, false));
			return Flux.just(serviceInstances).doOnNext(x -> {
				counter.incrementAndGet();
			});
		}

	}

}
