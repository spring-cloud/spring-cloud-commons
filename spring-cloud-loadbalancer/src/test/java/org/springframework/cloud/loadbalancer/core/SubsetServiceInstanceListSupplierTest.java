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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SubsetServiceInstanceListSupplier}
 *
 * @author Zhuozhi Ji
 */
class SubsetServiceInstanceListSupplierTest {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private MockEnvironment env;

	@BeforeEach
	public void setup() {
		env = new MockEnvironment();
	}

	@AfterEach
	public void destroy() {
		env = null;
	}

	@Test
	void shouldReturnEmptyWhenDelegateReturnedEmpty() {
		when(delegate.get()).thenReturn(Flux.just(Collections.emptyList()));
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar", 100));

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).isEmpty();
	}

	@Test
	void shouldReturnSublistWithGivenSubsetSize() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar", 5));

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).hasSize(5);
	}

	@Test
	void shouldReturnRawWhenLessThanSubsetSize() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));
		SubsetServiceInstanceListSupplier supplier = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar", 1000));

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).hasSize(101);
	}

	@Test
	void shouldReturnSameSublistForSameInstanceId() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));

		SubsetServiceInstanceListSupplier supplier1 = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar", 5));
		List<ServiceInstance> serviceInstances1 = Objects.requireNonNull(supplier1.get().blockFirst());

		SubsetServiceInstanceListSupplier supplier2 = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar", 5));
		List<ServiceInstance> serviceInstances2 = Objects.requireNonNull(supplier2.get().blockFirst());

		assertThat(serviceInstances1).isEqualTo(serviceInstances2);
	}

	@Test
	void shouldReturnDifferentSublistForDifferentInstanceId() {
		List<ServiceInstance> instances = IntStream.range(0, 101)
				.mapToObj(i -> new DefaultServiceInstance(Integer.toString(i), "test", "host" + i, 8080, false, null))
				.collect(Collectors.toList());

		when(delegate.get()).thenReturn(Flux.just(instances));

		SubsetServiceInstanceListSupplier supplier1 = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar1", 5));
		List<ServiceInstance> serviceInstances1 = Objects.requireNonNull(supplier1.get().blockFirst());

		SubsetServiceInstanceListSupplier supplier2 = new SubsetServiceInstanceListSupplier(delegate, env,
				subsetProperties("foobar2", 5));
		List<ServiceInstance> serviceInstances2 = Objects.requireNonNull(supplier2.get().blockFirst());

		assertThat(serviceInstances1).isNotEqualTo(serviceInstances2);
	}

	LoadBalancerProperties subsetProperties(String instanceId, int size) {
		LoadBalancerProperties properties = new LoadBalancerProperties();
		LoadBalancerProperties.Subset subset = new LoadBalancerProperties.Subset();
		subset.setInstanceId(instanceId);
		subset.setSize(size);
		properties.setSubset(subset);
		return properties;
	}

}
