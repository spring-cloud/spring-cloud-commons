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

	private SmoothServiceInstanceSelector selector;

	private volatile int position;

	/* for testing */ ServiceInstance[] expanded;

	private final Object expandingLock = new Object();

	LazyWeightedServiceInstanceList(List<ServiceInstance> instances, int[] weights) {
		this.init(instances, weights);
	}

	private void init(List<ServiceInstance> instances, int[] weights) {
		// Calculate the greatest common divisor (GCD) of weights, and the
		// total number of elements after expansion.
		int gcd = 0;
		int total = 0;
		for (int weight : weights) {
			gcd = greatestCommonDivisor(gcd, weight);
			total += weight;
		}
		this.selector = new SmoothServiceInstanceSelector(instances, weights, gcd);
		this.position = 0;
		this.expanded = new ServiceInstance[total / gcd];
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

	static class SmoothServiceInstanceSelector {

		static final int MODE_LIST = 0;

		static final int MODE_QUEUE = 1;

		final List<ServiceInstance> instances;

		final int[] weights;

		final Queue<Entry> queue;

		final int gcd;

		int mode;

		int position;

		SmoothServiceInstanceSelector(List<ServiceInstance> instances, int[] weights, int gcd) {
			this.instances = instances;
			this.weights = weights;
			this.queue = new ArrayDeque<>(instances.size());
			this.gcd = gcd;
			this.mode = MODE_LIST;
			this.position = 0;
		}

		ServiceInstance next() {
			if (mode == MODE_LIST) {
				ServiceInstance instance = instances.get(position);
				int weight = weights[position];
				position++;
				if (position == instances.size()) {
					mode = MODE_QUEUE;
					position = 0;
				}
				weight = weight - gcd;
				if (weight > 0) {
					queue.add(new Entry(instance, weight));
				}
				return instance;
			}
			else {
				if (queue.isEmpty()) {
					mode = MODE_LIST;
					return next(); // only recursive once.
				}

				Entry entry = queue.poll();
				entry.weight = entry.weight - gcd;
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
