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
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.util.MultiValueMap;

/**
 * A session cookie based implementation of {@link ServiceInstanceListSupplier} that gives
 * preference to the instance with an id specified in a request cookie.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class RequestBasedStickySessionServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final Log LOG = LogFactory.getLog(RequestBasedStickySessionServiceInstanceListSupplier.class);

	private final LoadBalancerProperties properties;

	/**
	 * @deprecated in favour of
	 * {@link RequestBasedStickySessionServiceInstanceListSupplier#RequestBasedStickySessionServiceInstanceListSupplier(ServiceInstanceListSupplier, ReactiveLoadBalancer.Factory)}
	 */
	@Deprecated
	public RequestBasedStickySessionServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties properties) {
		super(delegate);
		this.properties = properties;
	}

	public RequestBasedStickySessionServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		super(delegate);
		this.properties = loadBalancerClientFactory.getProperties(getServiceId());
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		String instanceIdCookieName = properties.getStickySession().getInstanceIdCookieName();
		Object context = request.getContext();
		if ((context instanceof RequestDataContext)) {
			MultiValueMap<String, String> cookies = ((RequestDataContext) context).getClientRequest().getCookies();
			if (cookies == null) {
				return delegate.get(request);
			}
			// We expect there to be one value in this cookie
			String cookie = cookies.getFirst(instanceIdCookieName);
			if (cookie != null) {
				return delegate.get(request).map(serviceInstances -> selectInstance(serviceInstances, cookie));
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Cookie not found. Returning all instances returned by delegate.");
			}
			return delegate.get(request);
		}
		// If the object type is not RequestData, we return all the instances provided by
		// the delegate.
		return delegate.get(request);
	}

	private List<ServiceInstance> selectInstance(List<ServiceInstance> serviceInstances, String cookie) {
		for (ServiceInstance serviceInstance : serviceInstances) {
			if (cookie.equals(serviceInstance.getInstanceId())) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Returning the service instance: %s. Found for cookie: %s",
							serviceInstance.toString(), cookie));
				}
				return Collections.singletonList(serviceInstance);
			}
		}
		// If the instances cannot be found based on the cookie,
		// we return all the instances provided by the delegate.
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format(
					"Service instance for cookie: %s not found. Returning all instances returned by delegate.",
					cookie));
		}
		return serviceInstances;
	}

}
