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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SameInstancePreferenceServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
class SameInstancePreferenceServiceInstanceListSupplierTests {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private final LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);

	private SameInstancePreferenceServiceInstanceListSupplier supplier;

	private final ServiceInstance first = serviceInstance("test-1");

	private final ServiceInstance second = serviceInstance("test-2");

	private final ServiceInstance third = serviceInstance("test-3");

	@BeforeEach
	void setUp() {
		LoadBalancerProperties properties = new LoadBalancerProperties();
		properties.setCallGetWithRequestOnDelegates(true);
		when(loadBalancerClientFactory.getProperties(any())).thenReturn(properties);
		supplier = new SameInstancePreferenceServiceInstanceListSupplier(delegate, loadBalancerClientFactory);
	}

	@Test
	void shouldReturnPreviouslySelectedInstanceIfAvailable() {
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third)));
		supplier.selectedServiceInstance(first);

		List<ServiceInstance> instances = supplier.get().blockFirst();

		assertThat(instances).hasSize(1);
		assertThat(instances.get(0)).isEqualTo(first);
	}

	@Test
	void shouldReturnAllInstancesFromDelegateIfNoPreviouslySelectedInstance() {
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third)));

		List<ServiceInstance> instances = supplier.get().blockFirst();

		assertThat(instances).hasSize(3);
	}

	@Test
	void shouldReturnAllInstancesFromDelegateIfPreviouslySelectedInstanceIsNotAvailable() {
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(second, third)));
		supplier.selectedServiceInstance(first);

		List<ServiceInstance> instances = supplier.get().blockFirst();

		assertThat(instances).hasSize(2);
	}

	@Test
	void shouldCallGetRequestOnDelegate() {
		Request<DefaultRequestContext> request = new DefaultRequest<>(new DefaultRequestContext());
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third)));
		when(delegate.get(request)).thenReturn(Flux.just(Arrays.asList(first, second)));

		List<ServiceInstance> instances = supplier.get(request).blockFirst();

		assertThat(instances).hasSize(2);
	}

	private DefaultServiceInstance serviceInstance(String instanceId) {
		return new DefaultServiceInstance(instanceId, "test", "http://test.test", 9080, false);
	}

}
