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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import static java.util.stream.Collectors.summingInt;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LazyWeightedServiceInstanceList}.
 *
 * @author Zhuozhi Ji
 */
class LazyWeightedServiceInstanceListTest {

	@Test
	void shouldCreateListWithSizeEqualToSumofRatio() {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		int[] weights = new int[10];
		for (int i = 0; i < 10; i++) {
			int weight = (1 << i) * 100;
			weights[i] = weight;
			serviceInstances.add(serviceInstance("test-" + i, buildWeightMetadata(weight)));
		}

		int total = Arrays.stream(weights).sum() / 100;
		List<ServiceInstance> list = new LazyWeightedServiceInstanceList(serviceInstances, weights);
		assertThat(list).hasSize(total);
	}

	@Test
	void shouldFillListWithAllNullElementsIfNotAccessed() {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		int[] weights = new int[10];
		for (int i = 0; i < 10; i++) {
			int weight = (1 << i) * 100;
			weights[i] = weight;
			serviceInstances.add(serviceInstance("test-" + i, buildWeightMetadata(weight)));
		}

		LazyWeightedServiceInstanceList list = new LazyWeightedServiceInstanceList(serviceInstances, weights);
		for (int i = 0; i < list.size(); i++) {
			assertThat(list.expanded[i]).isNull();
		}
	}

	@Test
	void shouldFillAllElementsIfGreaterPositionAccessed() {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		int[] weights = new int[10];
		for (int i = 0; i < 10; i++) {
			int weight = (1 << i) * 100;
			weights[i] = weight;
			serviceInstances.add(serviceInstance("test-" + i, buildWeightMetadata(weight)));
		}

		LazyWeightedServiceInstanceList list = new LazyWeightedServiceInstanceList(serviceInstances, weights);
		list.get(list.size() - 1);
		for (int i = 0; i < list.size(); i++) {
			assertThat(list.expanded[i]).isNotNull();
		}
	}

	@Test
	void shouldFillAllElementsCorrectlyIfConcurrentRandomAccess() throws InterruptedException {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		int[] weights = new int[10];
		for (int i = 0; i < 10; i++) {
			int weight = 1 << i;
			weights[i] = weight;
			serviceInstances.add(serviceInstance("test-" + i, buildWeightMetadata(weight)));
		}

		Random random = new Random();
		int processors = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(processors, processors, 1, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>());
		LazyWeightedServiceInstanceList list = new LazyWeightedServiceInstanceList(serviceInstances, weights);

		CountDownLatch countDownLatch = new CountDownLatch(list.size());
		for (int i = 0; i < list.size(); i++) {
			int p = random.nextInt(list.size());
			executor.execute(() -> {
				list.get(p);
				countDownLatch.countDown();
			});
		}
		countDownLatch.await();

		// make sure all instances are expanded
		list.get(list.size() - 1);

		Map<String, Integer> counter = Arrays.stream(list.expanded)
				.collect(Collectors.groupingBy(ServiceInstance::getInstanceId, summingInt(e -> 1)));
		for (int i = 0; i < 10; i++) {
			assertThat(counter).containsEntry(serviceInstances.get(i).getInstanceId(), weights[i]);
		}
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
