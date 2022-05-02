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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Zhuozhi Ji
 */
class WeightedLoadBalancerTests {

	static final double DELTA = 1e-6;

	@Test
	void shouldGetEmptyResponseWhenEmptyServiceInstanceList() {
		WeightedLoadBalancer weightedLoadBalancer = weightedLoadBalancer();
		Response<ServiceInstance> chosenInstance = weightedLoadBalancer.choose().block();
		assertThat(Objects.requireNonNull(chosenInstance).hasServer()).isFalse();
	}

	@Test
	void shouldMeetsExpectedPercentageOfIncreasing() {
		int[] weights = IntStream.iterate(1, i -> i + 1).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
	}

	@Test
	void shouldMeetsExpectedPercentageOfPow2() {
		int[] weights = IntStream.iterate(1, i -> i + 1).map(i -> 1 << (i - 1)).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
	}

	@Test
	void shouldMeetsExpectedPercentageOfPrimes() {
		int[] weights = IntStream.iterate(1, i -> i + 1).filter(WeightedLoadBalancerTests::isPrime).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
	}

	@Test
	void shouldMeetsExpectedPercentageOfRandoms() {
		Random random = new Random();
		int[] weights = IntStream.iterate(1, i -> i + 1).map(i -> random.nextInt(100)).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
	}

	static boolean isPrime(int n) {
		for (int i = 2; i <= (int) Math.sqrt(n); i++) {
			if (n % i == 0) {
				return true;
			}
		}
		return false;
	}

	void assertMeetsExceptedPercentage(int[] weights, int total) {
		WeightedLoadBalancer loadBalancer = weightedLoadBalancer(weights);

		long[] distribution = new long[weights.length];
		for (int i = 0; i < total; i++) {
			ServiceInstance instance = Objects.requireNonNull(loadBalancer.choose().block()).getServer();
			distribution[Integer.parseInt(instance.getInstanceId())]++;
		}

		checkPercentage(weights, distribution);
	}

	void checkPercentage(int[] weights, long[] distribution) {

		long totalWeight = Arrays.stream(weights).sum();
		long totalDistribution = Arrays.stream(distribution).sum();

		for (int i = 0; i < weights.length; i++) {
			double percentageWeight = 1.0 * weights[i] / totalWeight;
			double percentageDistribution = 1.0 * distribution[i] / totalDistribution;
			assertThat(Math.abs(percentageWeight - percentageDistribution) <= DELTA).isTrue();
		}
	}

	WeightedLoadBalancer weightedLoadBalancer(int... weights) {
		List<ServiceInstance> instances = instancesWithWeight(weights);

		return new WeightedLoadBalancer(serviceInstanceListSupplierProvider(instances), UUID.randomUUID().toString());
	}

	ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider(List<ServiceInstance> instances) {
		SameInstancePreferenceServiceInstanceListSupplier supplier = mock(
				SameInstancePreferenceServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(instances));
		return new SimpleObjectProvider<>(supplier);
	}

	List<ServiceInstance> instancesWithWeight(int... weights) {
		List<ServiceInstance> instances = new ArrayList<>();
		for (int i = 0; i < weights.length; i++) {
			DefaultServiceInstance instance = new DefaultServiceInstance();
			instance.setInstanceId(i + "");
			instance.setWeight(weights[i]);
			instances.add(instance);
		}
		return instances;
	}

}