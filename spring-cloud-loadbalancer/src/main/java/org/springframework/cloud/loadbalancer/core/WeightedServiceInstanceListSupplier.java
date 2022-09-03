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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A {@link ServiceInstanceListSupplier} implementation that uses weights to expand the
 * instances provided by delegate.
 *
 * @author Zhuozhi Ji
 */
public class WeightedServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final Log LOG = LogFactory.getLog(WeightedServiceInstanceListSupplier.class);

	static final String METADATA_WEIGHT_KEY = "weight";

	static final int DEFAULT_WEIGHT = 1;

	private final WeightFunction weightFunction;

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		super(delegate);
		this.weightFunction = WeightedServiceInstanceListSupplier::metadataWeightFunction;
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, WeightFunction weightFunction) {
		super(delegate);
		this.weightFunction = weightFunction;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(this::expandByWeight);
	}

	private List<ServiceInstance> expandByWeight(List<ServiceInstance> instances) {
		if (instances.size() == 0) {
			return instances;
		}

		int[] weights = instances.stream().mapToInt(instance -> {
			try {
				int weight = weightFunction.apply(instance);
				if (weight <= 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format(
								"The weight of the instance %s should be a positive integer, but it got %d, using %d as default",
								instance.getInstanceId(), weight, DEFAULT_WEIGHT));
					}
					return DEFAULT_WEIGHT;
				}
				return weight;
			}
			catch (Exception e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format(
							"Exception occurred during apply weight function to instance %s, using %d as default",
							instance.getInstanceId(), DEFAULT_WEIGHT), e);
				}
				return DEFAULT_WEIGHT;
			}
		}).toArray();

		int gcd = weights[0];
		int total = weights[0];
		for (int i = 1; i < weights.length; i++) {
			gcd = gcd(gcd, weights[i]);
			total += weights[i];
		}

		List<ServiceInstance> newInstances = new ArrayList<>(total / gcd);

		// use iterator for some implementation of the List that not supports
		// RandomAccess,
		// but weights is supported, so use a local variable `i` to get the current
		// position.
		int i = 0;
		for (ServiceInstance instance : instances) {
			weights[i] /= gcd;
			for (int weight = 0; weight < weights[i]; weight++) {
				newInstances.add(instance);
			}
			i++;
		}

		Collections.shuffle(newInstances);
		return newInstances;
	}

	static int metadataWeightFunction(ServiceInstance serviceInstance) {
		Map<String, String> metadata = serviceInstance.getMetadata();
		if (metadata != null) {
			String weightValue = metadata.get(METADATA_WEIGHT_KEY);
			if (weightValue != null) {
				return Integer.parseInt(weightValue);
			}
		}
		return DEFAULT_WEIGHT;
	}

	static int gcd(int a, int b) {
		int r;
		while (b != 0) {
			r = a % b;
			a = b;
			b = r;
		}
		return a;
	}

}
