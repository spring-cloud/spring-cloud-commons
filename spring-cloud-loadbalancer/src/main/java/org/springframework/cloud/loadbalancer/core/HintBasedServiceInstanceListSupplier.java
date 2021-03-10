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

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.HintRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * A {@link ServiceInstanceListSupplier} implementation that uses hints to filter service
 * instances provided by the delegate.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.2
 */
public class HintBasedServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private final LoadBalancerProperties properties;

	public HintBasedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties properties) {
		super(delegate);
		this.properties = properties;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get();
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		if (request.getContext() == null) {
			return get();
		}
		if (request.getContext() instanceof RequestDataContext) {
			RequestDataContext context = (RequestDataContext) request.getContext();
			if (context.getClientRequest() != null) {
				HttpHeaders headers = context.getClientRequest().getHeaders();
				if (headers != null) {
					String hintFromHeader = headers.getFirst(properties.getHintHeaderName());
					if (StringUtils.hasText(hintFromHeader)) {
						return get().map(instances -> filteredByHint(instances, hintFromHeader));
					}
				}
			}
		}
		if (request.getContext() instanceof HintRequestContext) {
			HintRequestContext context = (HintRequestContext) request.getContext();
			String hintFromProperties = context.getHint();
			if (StringUtils.hasText(hintFromProperties)) {
				return get().map(instances -> filteredByHint(instances, hintFromProperties));
			}
		}
		return get();
	}

	private List<ServiceInstance> filteredByHint(List<ServiceInstance> instances, String hint) {
		List<ServiceInstance> filteredInstances = new ArrayList<>();
		for (ServiceInstance serviceInstance : instances) {
			if (serviceInstance.getMetadata().getOrDefault("hint", "").equals(hint)) {
				filteredInstances.add(serviceInstance);
			}
		}
		if (filteredInstances.size() > 0) {
			return filteredInstances;
		}

		// If instances cannot be found based on hint,
		// we return all instances retrieved for given service id.
		return instances;
	}

}
