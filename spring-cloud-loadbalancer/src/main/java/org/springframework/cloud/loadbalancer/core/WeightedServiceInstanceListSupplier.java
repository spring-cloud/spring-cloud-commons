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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

/**
 * A {@link ServiceInstanceListSupplier} implementation that uses weights to expand the
 * instances provided by delegate.
 *
 * @author Zhuozhi Ji
 * @author Olga Maciaszek-Sharma
 */
public class WeightedServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final Log LOG = LogFactory.getLog(WeightedServiceInstanceListSupplier.class);

	static final String METADATA_WEIGHT_KEY = "weight";

	static final int DEFAULT_WEIGHT = 1;

	private final WeightFunction weightFunction;

	private boolean callGetWithRequestOnDelegates;

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		this(delegate, WeightedServiceInstanceListSupplier::metadataWeightFunction);
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, WeightFunction weightFunction) {
		super(delegate);
		this.weightFunction = weightFunction;
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		this(delegate, WeightedServiceInstanceListSupplier::metadataWeightFunction, loadBalancerClientFactory);
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, WeightFunction weightFunction,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		super(delegate);
		this.weightFunction = weightFunction;
		callGetWithRequestOnDelegates = loadBalancerClientFactory.getProperties(getServiceId())
			.isCallGetWithRequestOnDelegates();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(this::expandByWeight);
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		if (callGetWithRequestOnDelegates) {
			return delegate.get(request).map(this::expandByWeight);
		}
		return get();
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

		return new LazyWeightedServiceInstanceList(instances, weights);
	}

	static int metadataWeightFunction(ServiceInstance serviceInstance) {
		Map<String, String> metadata = serviceInstance.getMetadata();
		if (metadata != null) {
			String weightValue = metadata.get(METADATA_WEIGHT_KEY);
			if (weightValue != null) {
				return Integer.parseInt(weightValue);
			}
		}
		// using default weight when metadata is missing or
		// weight is not specified
		return DEFAULT_WEIGHT;
	}

}
