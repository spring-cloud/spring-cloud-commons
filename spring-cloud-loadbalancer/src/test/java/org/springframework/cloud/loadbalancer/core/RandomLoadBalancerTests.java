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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RandomLoadBalancer}.
 *
 * @author Olga Maciaszek-Sharma
 */
class RandomLoadBalancerTests {

	private final ServiceInstance serviceInstance = new DefaultServiceInstance();

	private RandomLoadBalancer loadBalancer;

	@Test
	void shouldReturnOneServiceInstance() {
		DiscoveryClientServiceInstanceListSupplier supplier = mock(DiscoveryClientServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(Arrays.asList(serviceInstance, new DefaultServiceInstance())));
		loadBalancer = new RandomLoadBalancer(new SimpleObjectProvider<>(supplier), "test");

		Response<ServiceInstance> response = loadBalancer.choose().block();

		assertThat(response.hasServer()).isTrue();
	}

	@Test
	void shouldReturnEmptyResponseWhenSupplierNotAvailable() {
		loadBalancer = new RandomLoadBalancer(new SimpleObjectProvider<>(null), "test");

		Response<ServiceInstance> response = loadBalancer.choose().block();

		assertThat(response.hasServer()).isFalse();
	}

	@Test
	void shouldReturnEmptyResponseWhenNoInstancesAvailable() {
		DiscoveryClientServiceInstanceListSupplier supplier = mock(DiscoveryClientServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(Collections.emptyList()));
		loadBalancer = new RandomLoadBalancer(new SimpleObjectProvider<>(supplier), "test");

		Response<ServiceInstance> response = loadBalancer.choose().block();

		assertThat(response.hasServer()).isFalse();
	}

	@Test
	void shouldTriggerSelectedInstanceCallback() {
		SameInstancePreferenceServiceInstanceListSupplier supplier = mock(
				SameInstancePreferenceServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(Collections.singletonList(serviceInstance)));
		loadBalancer = new RandomLoadBalancer(new SimpleObjectProvider<>(supplier), "test");

		Response<ServiceInstance> response = loadBalancer.choose().block();

		assertThat(response.hasServer()).isTrue();
		assertThat(response.getServer()).isEqualTo(serviceInstance);
		verify((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstance);
	}

}
