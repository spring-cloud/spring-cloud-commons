/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;

/**
 * A {@link ServiceInstanceListSupplier} implementation that avoids picking the same
 * service instance while retrying requests.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class RetryAwareServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private final Log LOG = LogFactory.getLog(RetryAwareServiceInstanceListSupplier.class);

	public RetryAwareServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		super(delegate);
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		if (!(request.getContext() instanceof RetryableRequestContext)) {
			return delegate.get(request);
		}
		RetryableRequestContext context = (RetryableRequestContext) request.getContext();
		ServiceInstance previousServiceInstance = context.getPreviousServiceInstance();
		if (previousServiceInstance == null) {
			return delegate.get(request);
		}
		return delegate.get(request).map(instances -> filteredByPreviousInstance(instances, previousServiceInstance));
	}

	private List<ServiceInstance> filteredByPreviousInstance(List<ServiceInstance> instances,
			ServiceInstance previousServiceInstance) {
		List<ServiceInstance> filteredInstances = new ArrayList<>(instances);
		if (previousServiceInstance != null) {
			filteredInstances.remove(previousServiceInstance);
		}
		if (filteredInstances.size() > 0) {
			return filteredInstances;
		}
		if (LOG.isWarnEnabled()) {
			LOG.warn(String.format(
					"No instances found after removing previously used service instance from the search (%s). Returning all found instances.",
					previousServiceInstance));
		}
		return instances;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get();
	}

}
