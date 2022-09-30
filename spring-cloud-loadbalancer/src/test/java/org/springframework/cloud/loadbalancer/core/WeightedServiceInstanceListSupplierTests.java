/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import static java.util.stream.Collectors.summingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.loadbalancer.core.WeightedServiceInstanceListSupplier.DEFAULT_WEIGHT;

/**
 * Tests for {@link WeightedServiceInstanceListSupplier}.
 *
 * @author Zhuozhi Ji
 */
class WeightedServiceInstanceListSupplierTests {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	@Test
	void shouldReturnEmptyWhenDelegateReturnedEmpty() {
		when(delegate.get()).thenReturn(Flux.just(Collections.emptyList()));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		assertThat(serviceInstances).isEmpty();
	}

	@Test
	void shouldBeSameAsWeightsRatioWhenGcdOfWeightsIs1() {
		ServiceInstance one = serviceInstance("test-1", buildWeightMetadata(1));
		ServiceInstance two = serviceInstance("test-2", buildWeightMetadata(2));
		ServiceInstance three = serviceInstance("test-3", buildWeightMetadata(3));

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", 1);
		assertThat(counter).containsEntry("test-2", 2);
		assertThat(counter).containsEntry("test-3", 3);
	}

	@Test
	void shouldBeSameAsWeightsRatioWhenGcdOfWeightsIs10() {
		ServiceInstance one = serviceInstance("test-1", buildWeightMetadata(10));
		ServiceInstance two = serviceInstance("test-2", buildWeightMetadata(20));
		ServiceInstance three = serviceInstance("test-3", buildWeightMetadata(30));

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", 1);
		assertThat(counter).containsEntry("test-2", 2);
		assertThat(counter).containsEntry("test-3", 3);
	}

	@Test
	void shouldUseDefaultWeightWhenWeightNotSpecified() {
		ServiceInstance one = serviceInstance("test-1", Collections.emptyMap());
		ServiceInstance two = serviceInstance("test-2", Collections.emptyMap());
		ServiceInstance three = serviceInstance("test-3", buildWeightMetadata(3));

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-2", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-3", 3);
	}

	@Test
	void shouldUseDefaultWeightWhenWeightIsNotNumber() {
		ServiceInstance one = serviceInstance("test-1", buildWeightMetadata("Foo"));
		ServiceInstance two = serviceInstance("test-2", buildWeightMetadata("Bar"));
		ServiceInstance three = serviceInstance("test-3", buildWeightMetadata("Baz"));

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-2", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-3", DEFAULT_WEIGHT);
	}

	@Test
	void shouldUseDefaultWeightWhenWeightedFunctionReturnedZero() {
		ServiceInstance one = serviceInstance("test-1", Collections.emptyMap());
		ServiceInstance two = serviceInstance("test-2", Collections.emptyMap());
		ServiceInstance three = serviceInstance("test-3", Collections.emptyMap());

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate, instance -> 0);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-2", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-3", DEFAULT_WEIGHT);
	}

	@Test
	void shouldUseDefaultWeightWhenWeightedFunctionReturnedNegative() {
		ServiceInstance one = serviceInstance("test-1", Collections.emptyMap());
		ServiceInstance two = serviceInstance("test-2", Collections.emptyMap());
		ServiceInstance three = serviceInstance("test-3", Collections.emptyMap());

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate,
				instance -> -1);

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-2", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-3", DEFAULT_WEIGHT);
	}

	@Test
	void shouldUseDefaultWeightWhenWeightedFunctionThrownException() {
		ServiceInstance one = serviceInstance("test-1", Collections.emptyMap());
		ServiceInstance two = serviceInstance("test-2", Collections.emptyMap());
		ServiceInstance three = serviceInstance("test-3", Collections.emptyMap());

		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(one, two, three)));
		WeightedServiceInstanceListSupplier supplier = new WeightedServiceInstanceListSupplier(delegate, instance -> {
			throw new RuntimeException();
		});

		List<ServiceInstance> serviceInstances = Objects.requireNonNull(supplier.get().blockFirst());
		Map<String, Integer> counter = serviceInstances.stream()
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		assertThat(counter).containsEntry("test-1", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-2", DEFAULT_WEIGHT);
		assertThat(counter).containsEntry("test-3", DEFAULT_WEIGHT);
	}

	private ServiceInstance serviceInstance(String instanceId, Map<String, String> metadata) {
		return new DefaultServiceInstance(instanceId, "test", "localhost", 8080, false, metadata);
	}

	private Map<String, String> buildWeightMetadata(Object weight) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("weight", weight.toString());
		return metadata;
	}

}
