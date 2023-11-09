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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * A {@link ServiceInstanceListSupplier} implementation that uses
 * <a href="https://sre.google/sre-book/load-balancing-datacenter/">deterministic
 * subsetting algorithm</a> to limit the number of instances provided by delegate.
 *
 * @author Zhuozhi Ji
 * @since 4.1.0
 */
public class SubsetServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private final String instanceId;

	private final int size;

	public SubsetServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, PropertyResolver resolver,
			ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
		super(delegate);
		LoadBalancerProperties properties = factory.getProperties(getServiceId());
		this.instanceId = resolveInstanceId(properties, resolver);
		this.size = properties.getSubset().getSize();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(instances -> {
			if (instances.size() <= size) {
				return instances;
			}

			instances = new ArrayList<>(instances);

			int instanceId = this.instanceId.hashCode() & Integer.MAX_VALUE;
			int count = instances.size() / size;
			int round = instanceId / count;

			Random random = new Random(round);
			Collections.shuffle(instances, random);

			int bucket = instanceId % count;
			int start = bucket * size;
			return instances.subList(start, start + size);
		});
	}

	private static String resolveInstanceId(LoadBalancerProperties properties, PropertyResolver resolver) {
		String instanceId = properties.getSubset().getInstanceId();
		if (StringUtils.hasText(instanceId)) {
			return resolver.resolvePlaceholders(properties.getSubset().getInstanceId());
		}
		return IdUtils.getDefaultInstanceId(resolver);
	}

	public String getInstanceId() {
		return instanceId;
	}

	public int getSize() {
		return size;
	}

}
