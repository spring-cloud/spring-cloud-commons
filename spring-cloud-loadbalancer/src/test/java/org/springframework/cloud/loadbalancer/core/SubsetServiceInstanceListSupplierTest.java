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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubsetServiceInstanceListSupplierTest {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	@Test
	void shouldConstructCorrect() {
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate);
		assertThat(supplier.instanceId).isNotNull();
		assertThat(supplier.subsetSize).isEqualTo(SubsetServiceInstanceListSupplier.DEFAULT_SUBSET_SIZE);

		supplier = new SubsetServiceInstanceListSupplier(delegate, 11);
		assertThat(supplier.instanceId).isNotNull();
		assertThat(supplier.subsetSize).isEqualTo(11);

		supplier = new SubsetServiceInstanceListSupplier(delegate, "foobar", 11);
		assertThat(supplier.instanceId).isEqualTo("foobar");
		assertThat(supplier.subsetSize).isEqualTo(11);
	}

	@Test
	void shouldReturnEmptyWhenDelegateReturnedEmpty() {
		when(delegate.get()).thenReturn(Flux.just(Collections.emptyList()));
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).isEmpty();
	}

	@Test
	void shouldReturnSublistWithGivenSubsetSize() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate, 5);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).hasSize(5);
	}

	@Test
	void shouldReturnRawWhenLessThanSubsetSize() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate, 1000);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).hasSize(101);
	}

	@Test
	void shouldReturnSameSublistForSameInstanceId() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));

		SubsetServiceInstanceListSupplier supplier1 = new SubsetServiceInstanceListSupplier(delegate, "foobar", 5);
		List<ServiceInstance> serviceInstances1 = Objects.requireNonNull(supplier1.get().blockFirst());

		SubsetServiceInstanceListSupplier supplier2 = new SubsetServiceInstanceListSupplier(delegate, "foobar", 5);
		List<ServiceInstance> serviceInstances2 = Objects.requireNonNull(supplier2.get().blockFirst());

		assertThat(serviceInstances1).isEqualTo(serviceInstances2);
	}

	@Test
	void shouldReturnDifferentSublistForDifferentInstanceId() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));

		SubsetServiceInstanceListSupplier supplier1 = new SubsetServiceInstanceListSupplier(delegate, "foobar1", 5);
		List<ServiceInstance> serviceInstances1 = Objects.requireNonNull(supplier1.get().blockFirst());

		SubsetServiceInstanceListSupplier supplier2 = new SubsetServiceInstanceListSupplier(delegate, "foobar2", 5);
		List<ServiceInstance> serviceInstances2 = Objects.requireNonNull(supplier2.get().blockFirst());

		assertThat(serviceInstances1).isNotEqualTo(serviceInstances2);
	}

}
