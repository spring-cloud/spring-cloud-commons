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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.WeightedServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;

/**
 * A Weighted implementation of {@link ReactorServiceInstanceLoadBalancer}.
 *
 * @author Zhuozhi JI
 */
public class WeightedLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(WeightedLoadBalancer.class);

	private static final int DEFAULT_WEIGHT = 1;

	private static final String METADATA_WEIGHT_KEY = "weight";

	/**
	 * Maximum number of samples each time an instance with the largest weight is chosen.
	 * <p>
	 * This value is a trade-off between weight distribution accuracy and performance. The
	 * larger this value is, the more accurate the distribution will be, but the
	 * corresponding performance will be worse. After modifying this value, unit tests
	 * must be performed to ensure the accuracy of weight distribution, and
	 * micro-benchmark tests must be performed to ensure that the performance impact is
	 * within an acceptable range.
	 */
	static final int MAX_CHOOSE_SAMPLES = 100;

	final Map<String, AtomicInteger> currentWeightMap = new ConcurrentHashMap<>();

	final String serviceId;

	final AtomicInteger position;

	ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

	ToIntFunction<ServiceInstance> weightFunction;

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 * @param weightFunction a function for calculating the weight of instance
	 * @param seedPosition the initial position used to peek the element with the largest
	 * weight
	 */
	public WeightedLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId, ToIntFunction<ServiceInstance> weightFunction, int seedPosition) {
		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
		this.serviceId = serviceId;
		this.weightFunction = weightFunction;
		this.position = new AtomicInteger(seedPosition);
	}

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 * @param weightFunction a function for calculating the weight of instance
	 */
	public WeightedLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId, ToIntFunction<ServiceInstance> weightFunction) {
		this(serviceInstanceListSupplierProvider, serviceId, weightFunction, new Random().nextInt(1000));
	}

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public WeightedLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId) {
		this(serviceInstanceListSupplierProvider, serviceId, null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
				.getIfAvailable(NoopServiceInstanceListSupplier::new);
		return supplier.get(request).next()
				.map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
	}

	private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
			List<ServiceInstance> serviceInstances) {
		Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
		if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
			((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
		}
		return serviceInstanceResponse;
	}

	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
		if (instances.isEmpty()) {
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}

		ServiceInstance best = null;
		AtomicInteger bestCurrentWeight = null;
		int bestUpdatedCurrentWeight = 0;
		int total = 0;

		int pos = this.position.incrementAndGet();
		int size = Math.min(instances.size(), MAX_CHOOSE_SAMPLES);

		for (int i = 0; i < size; i++) {
			int peek = ((pos + i) & Integer.MAX_VALUE) % instances.size();
			ServiceInstance instance = instances.get(peek);

			int weight = calculateWeight(instance);
			AtomicInteger currentWeight = calculateCurrentWeight(instance, weight);
			int updatedCurrentWeight = currentWeight.addAndGet(weight);
			total += weight;

			// Use bestUpdatedCurrentWeight to ensure strictly increasing.
			if (best == null || updatedCurrentWeight > bestUpdatedCurrentWeight) {
				best = instance;
				bestCurrentWeight = currentWeight;
				bestUpdatedCurrentWeight = updatedCurrentWeight;
			}
		}

		if (bestCurrentWeight != null) {
			bestCurrentWeight.getAndAdd(-total);
		}

		return new DefaultResponse(best);
	}

	int calculateWeight(ServiceInstance serviceInstance) {
		if (weightFunction != null) {
			return weightFunction.applyAsInt(serviceInstance);
		}
		if (serviceInstance instanceof WeightedServiceInstance) {
			return ((WeightedServiceInstance) serviceInstance).getWeight();
		}
		return metadataWeightFunction(serviceInstance);
	}

	AtomicInteger calculateCurrentWeight(ServiceInstance serviceInstance, int weight) {
		if (serviceInstance instanceof WeightedServiceInstance) {
			return ((WeightedServiceInstance) serviceInstance).getCurrentWeight();
		}
		return currentWeightMap.computeIfAbsent(serviceInstance.getInstanceId(),
				k -> new AtomicInteger(ThreadLocalRandom.current().nextInt(Math.abs(weight) + 1)));
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

}
