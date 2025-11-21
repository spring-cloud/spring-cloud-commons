/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A {@link List} implementation that lazily fills weighted {@link ServiceInstance}
 * objects.
 *
 * @author Zhuozhi Ji
 * @see WeightedServiceInstanceListSupplier
 */
class LazyWeightedServiceInstanceList extends AbstractList<ServiceInstance> {

	/* for testing */ final ServiceInstance[] expanded;

	private final Object expandingLock = new Object();

	private @Nullable WeightedServiceInstanceSelector selector;

	private volatile int position = 0;

	LazyWeightedServiceInstanceList(List<ServiceInstance> instances, int[] weights) {
		// Calculate the greatest common divisor (GCD) of weights, and the
		// total number of elements after expansion.
		int greatestCommonDivisor = 0;
		int total = 0;
		for (int weight : weights) {
			greatestCommonDivisor = greatestCommonDivisor(greatestCommonDivisor, weight);
			total += weight;
		}
		expanded = new ServiceInstance[total / greatestCommonDivisor];
		selector = new WeightedServiceInstanceSelector(instances, weights, greatestCommonDivisor);
	}

	@Override
	public ServiceInstance get(int index) {
		if (index >= position) {
			synchronized (expandingLock) {
				for (; position <= index && position < expanded.length; position++) {
					if (selector != null) {
						expanded[position] = selector.next();
					}
				}
				if (position == expanded.length) {
					selector = null; // for gc
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

	static class WeightedServiceInstanceSelector {

		Queue<Entry> active;

		Queue<Entry> expired;

		WeightedServiceInstanceSelector(List<ServiceInstance> instances, int[] weights, int greatestCommonDivisor) {
			active = new ArrayDeque<>(instances.size());
			expired = new ArrayDeque<>(instances.size());
			// Use iterator for some implementation of the List that not supports
			// RandomAccess, but `weights` is supported, so use a local variable `i`
			// to get the current position.
			int i = 0;
			for (ServiceInstance instance : instances) {
				active.offer(new Entry(instance, weights[i] / greatestCommonDivisor));
				i++;
			}
		}

		@SuppressWarnings("NullAway") // see comment below
		ServiceInstance next() {
			if (active.isEmpty()) {
				Queue<Entry> temp = active;
				active = expired;
				expired = temp;
			}

			Entry entry = active.poll();
			if (entry == null) {
				// Suppress warnings, never touched!
				return null;
			}

			entry.remainder--;
			if (entry.remainder == 0) {
				entry.remainder = entry.weight;
				expired.offer(entry);
			}
			else {
				active.offer(entry);
			}
			return entry.instance;
		}

		static class Entry {

			final ServiceInstance instance;

			final int weight;

			int remainder;

			Entry(ServiceInstance instance, int weight) {
				this.instance = instance;
				this.weight = weight;
				remainder = weight;
			}

		}

	}

}
