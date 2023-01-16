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
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A {@link List} implementation that weighted and lazy fills {@link ServiceInstance}.
 *
 * @author Zhuozhi Ji
 * @see WeightedServiceInstanceListSupplier
 */
class LazyWeightedServiceInstanceList extends AbstractList<ServiceInstance> {

	private final InterleavedWeightedServiceInstanceSelector selector;

	private volatile int position;

	/* for testing */ final ServiceInstance[] expanded;

	private final Object expandingLock = new Object();

	LazyWeightedServiceInstanceList(List<ServiceInstance> instances, int[] weights) {
		// Calculate the greatest common divisor (GCD) of weights, and the
		// total number of elements after expansion.
		int greatestCommonDivisor = 0;
		int total = 0;
		for (int weight : weights) {
			greatestCommonDivisor = greatestCommonDivisor(greatestCommonDivisor, weight);
			total += weight;
		}
		selector = new InterleavedWeightedServiceInstanceSelector(instances.toArray(new ServiceInstance[0]), weights,
				greatestCommonDivisor);
		position = 0;
		expanded = new ServiceInstance[total / greatestCommonDivisor];
	}

	@Override
	public ServiceInstance get(int index) {
		if (index >= position) {
			synchronized (expandingLock) {
				for (; position <= index && position < expanded.length; position++) {
					expanded[position] = selector.next();
				}
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

	static class InterleavedWeightedServiceInstanceSelector {

		static final int MODE_LIST = 0;

		static final int MODE_QUEUE = 1;

		final ServiceInstance[] instances;

		final int[] weights;

		final int greatestCommonDivisor;

		final Queue<Entry> queue;

		int mode;

		int position;

		InterleavedWeightedServiceInstanceSelector(ServiceInstance[] instances, int[] weights,
				int greatestCommonDivisor) {
			this.instances = instances;
			this.weights = weights;
			this.greatestCommonDivisor = greatestCommonDivisor;
			queue = new ArrayDeque<>(instances.length);
			mode = MODE_LIST;
			position = 0;
		}

		ServiceInstance next() {
			if (mode == MODE_LIST) {
				ServiceInstance instance = instances[position];
				int weight = weights[position];

				weight = weight - greatestCommonDivisor;
				if (weight > 0) {
					queue.add(new Entry(instance, weight));
				}

				position++;
				if (position == instances.length) {
					mode = MODE_QUEUE;
					position = 0;
				}

				return instance;
			}
			else {
				if (queue.isEmpty()) {
					mode = MODE_LIST;
					return next();
				}

				Entry entry = queue.poll();
				entry.weight = entry.weight - greatestCommonDivisor;
				if (entry.weight > 0) {
					queue.add(entry);
				}
				return entry.instance;
			}
		}

		static class Entry {

			final ServiceInstance instance;

			int weight;

			Entry(ServiceInstance instance, int weight) {
				this.instance = instance;
				this.weight = weight;
			}

		}

	}

}
