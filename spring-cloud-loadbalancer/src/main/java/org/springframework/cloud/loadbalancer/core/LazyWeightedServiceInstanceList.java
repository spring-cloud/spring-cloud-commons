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

import java.util.AbstractList;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A {@link List} implementation that lazily fills weighted {@link ServiceInstance}
 * objects.
 *
 * @author Zhuozhi Ji
 * @see WeightedServiceInstanceListSupplier
 */
class LazyWeightedServiceInstanceList extends AbstractList<ServiceInstance> {

	private final List<ServiceInstance> instances;

	private final int[] weights;

	private SmoothServiceInstanceSelector selector;

	private volatile int position;

	/* for testing */ ServiceInstance[] expanded;

	private final Object expandingLock = new Object();

	LazyWeightedServiceInstanceList(List<ServiceInstance> instances, int[] weights) {
		this.instances = instances;
		this.weights = weights;
		this.init();
	}

	private void init() {
		// Calculate the greatest common divisor (GCD) of weights, max weight and the
		// total number of elements after expansion.
		int greatestCommonDivisor = 0;
		int max = 0;
		int total = 0;
		for (int weight : weights) {
			greatestCommonDivisor = greatestCommonDivisor(greatestCommonDivisor, weight);
			max = Math.max(max, weight);
			total += weight;
		}
		selector = new SmoothServiceInstanceSelector(instances, weights, max, greatestCommonDivisor);
		position = 0;
		expanded = new ServiceInstance[total / greatestCommonDivisor];
	}

	@Override
	public ServiceInstance get(int index) {
		if (index < position) {
			return expanded[index];
		}
		synchronized (expandingLock) {
			for (; position <= index && position < expanded.length; position++) {
				expanded[position] = selector.next();
			}
		}
		return expanded[index];
	}

	@Override
	public int size() {
		return expanded.length;
	}

	static int greatestCommonDivisor(int a, int b) {
		int r;
		while (b != 0) {
			r = a % b;
			a = b;
			b = r;
		}
		return a;
	}

	static class SmoothServiceInstanceSelector {

		final List<ServiceInstance> instances;

		final int[] weights;

		final int maxWeight;

		final int gcd;

		int position;

		int currentWeight;

		SmoothServiceInstanceSelector(List<ServiceInstance> instances, int[] weights, int maxWeight, int gcd) {
			this.instances = instances;
			this.weights = weights;
			this.maxWeight = maxWeight;
			this.gcd = gcd;
			this.currentWeight = 0;
		}

		ServiceInstance next() {
			// The weight of all instances is greater than 0, so it must be able to exit
			// the loop.
			while (true) {
				for (int picked = position; picked < weights.length; picked++) {
					if (weights[picked] > currentWeight) {
						position = picked + 1;
						return instances.get(picked);
					}
				}
				position = 0;
				currentWeight = currentWeight + gcd;
				if (currentWeight >= maxWeight) {
					currentWeight = 0;
				}
			}
		}

	}

}
