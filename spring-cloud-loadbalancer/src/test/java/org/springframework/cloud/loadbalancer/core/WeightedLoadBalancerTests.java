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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

	static final double STRICT_DELTA = 1e-6;

	static final double LOOSE_DELTA = 1e-3;

	@Test
	void shouldGetEmptyResponseWhenEmptyServiceInstanceList() {
		WeightedLoadBalancer weightedLoadBalancer = weightedLoadBalancer();
		Response<ServiceInstance> chosenInstance = weightedLoadBalancer.choose().block();
		assertThat(Objects.requireNonNull(chosenInstance).hasServer()).isFalse();
	}

	@Test
	void shouldMeetsExpectedPercentageOfSame() throws InterruptedException {
		int[] weights = IntStream.iterate(1, i -> i).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
		assertMeetsExceptedPercentageWithConcurrency(weights, total, Runtime.getRuntime().availableProcessors());
	}

	@Test
	void shouldMeetsExpectedPercentageOfIncreasing() throws InterruptedException {
		int[] weights = IntStream.iterate(1, i -> i + 1).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
		assertMeetsExceptedPercentageWithConcurrency(weights, total, Runtime.getRuntime().availableProcessors());
	}

	@Test
	void shouldMeetsExpectedPercentageOfPow2() throws InterruptedException {
		int[] weights = IntStream.iterate(1, i -> i + 1).map(i -> 1 << (i - 1)).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
		assertMeetsExceptedPercentageWithConcurrency(weights, total, Runtime.getRuntime().availableProcessors());
	}

	@Test
	void shouldMeetsExpectedPercentageOfPrimes() throws InterruptedException {
		int[] weights = IntStream.iterate(1, i -> i + 1).filter(WeightedLoadBalancerTests::isPrime).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
		assertMeetsExceptedPercentageWithConcurrency(weights, total, Runtime.getRuntime().availableProcessors());
	}

	@Test
	void shouldMeetsExpectedPercentageOfRandoms() throws InterruptedException {
		Random random = new Random();
		int[] weights = IntStream.iterate(1, i -> i + 1).map(i -> random.nextInt(100)).limit(10).toArray();
		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
		assertMeetsExceptedPercentageWithConcurrency(weights, total, Runtime.getRuntime().availableProcessors());
	}

	@Test
	void shouldMeetsExpectedPercentageOfLargeSlice() throws InterruptedException {
		int[] partial = IntStream.iterate(1, i -> i + 1).limit(10).toArray();
		int[] weights = Arrays.copyOf(partial, WeightedLoadBalancer.MAX_CHOOSE_SAMPLES);
		for (int i = partial.length; i < weights.length; i += partial.length) {
			System.arraycopy(partial, 0, weights, i, partial.length);
		}

		int total = Arrays.stream(weights).sum();
		assertMeetsExceptedPercentage(weights, total);
		assertMeetsExceptedPercentageWithConcurrency(weights, total, Runtime.getRuntime().availableProcessors());
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

		checkPercentage(weights, distribution, STRICT_DELTA);
	}

	void assertMeetsExceptedPercentageWithConcurrency(int[] weights, int total, int threads)
			throws InterruptedException {
		WeightedLoadBalancer loadBalancer = weightedLoadBalancer(weights);

		CountDownLatch countDownLatch = new CountDownLatch(threads);
		AtomicLong[] distribution = Stream.generate(AtomicLong::new).limit(weights.length).toArray(AtomicLong[]::new);

		ExecutorService executorService = Executors.newFixedThreadPool(threads);

		for (int t = 0; t < threads; t++) {
			executorService.submit(() -> {
				for (int i = 0; i < total; i++) {
					ServiceInstance instance = Objects.requireNonNull(loadBalancer.choose().block()).getServer();
					distribution[Integer.parseInt(instance.getInstanceId())].getAndIncrement();
				}
				countDownLatch.countDown();
			});
		}
		countDownLatch.await();
		checkPercentage(weights, Arrays.stream(distribution).mapToLong(AtomicLong::get).toArray(), LOOSE_DELTA);
	}

	void checkPercentage(int[] weights, long[] distribution, double delta) {

		long totalWeight = Arrays.stream(weights).sum();
		long totalDistribution = Arrays.stream(distribution).sum();

		for (int i = 0; i < weights.length; i++) {
			double percentageWeight = 1.0 * weights[i] / totalWeight;
			double percentageDistribution = 1.0 * distribution[i] / totalDistribution;
			assertThat(Math.abs(percentageWeight - percentageDistribution)).isLessThanOrEqualTo(delta);
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
