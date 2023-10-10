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
import java.util.UUID;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A {@link ServiceInstanceListSupplier} implementation that uses
 * <a href="https://sre.google/sre-book/load-balancing-datacenter/">deterministic
 * subsetting</a> to limit the number of instances provided by delegate.
 *
 * @author Zhuozhi Ji
 */
public class SubsetServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	static final int DEFAULT_SUBSET_SIZE = 100;

	final String instanceId;

	final int subsetSize;

	public SubsetServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		this(delegate, DEFAULT_SUBSET_SIZE);
	}

	public SubsetServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, int subsetSize) {
		this(delegate, UUID.randomUUID().toString(), subsetSize);
	}

	public SubsetServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, String instanceId, int subsetSize) {
		super(delegate);
		this.instanceId = instanceId;
		this.subsetSize = subsetSize;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(instances -> {
			if (instances.isEmpty()) {
				return instances;
			}

			instances = new ArrayList<>(instances);

			int clientId = instanceId.hashCode() & Integer.MAX_VALUE;
			int subsetCount = instances.size() / subsetSize;
			int round = clientId / subsetCount;

			Random random = new Random(round);
			Collections.shuffle(instances, random);

			int subsetId = clientId % subsetCount;
			int start = subsetId * subsetSize;
			return instances.subList(start, start + subsetSize);
		});
	}

}
