/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;

/**
 * An implementation of {@link ServiceInstanceListSupplier} that selects the previously
 * chosen instance if it's available.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
public class SameInstancePreferenceServiceInstanceListSupplier extends
		DelegatingServiceInstanceListSupplier implements SelectedInstanceCallback {

	private static final Log LOG = LogFactory
			.getLog(SameInstancePreferenceServiceInstanceListSupplier.class);

	private ServiceInstance previouslyReturnedInstance;

	public SameInstancePreferenceServiceInstanceListSupplier(
			ServiceInstanceListSupplier delegate) {
		super(delegate);
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(this::filteredBySameInstancePreference);
	}

	private List<ServiceInstance> filteredBySameInstancePreference(
			List<ServiceInstance> serviceInstances) {
		if (previouslyReturnedInstance != null
				&& serviceInstances.contains(previouslyReturnedInstance)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format(
						"Returning previously selected service instance: %s",
						previouslyReturnedInstance));
			}
			return Collections.singletonList(previouslyReturnedInstance);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format(
					"Previously selected service instance %s was not available. Returning all the instances returned by delegate.",
					previouslyReturnedInstance));
		}
		previouslyReturnedInstance = null;
		return serviceInstances;
	}

	@Override
	public void selectedServiceInstance(ServiceInstance serviceInstance) {
		if (previouslyReturnedInstance == null
				|| !previouslyReturnedInstance.equals(serviceInstance)) {
			previouslyReturnedInstance = serviceInstance;
		}
	}

}
