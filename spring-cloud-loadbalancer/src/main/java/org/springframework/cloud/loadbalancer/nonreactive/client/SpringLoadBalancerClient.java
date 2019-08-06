/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.loadbalancer.nonreactive.client;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.util.ReflectionUtils;

/**
 * The default {@link LoadBalancerClient} implementation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class SpringLoadBalancerClient implements LoadBalancerClient {

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	public SpringLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request)
			throws IOException {
		ServiceInstance serviceInstance = Optional.ofNullable(choose(serviceId))
				.orElseThrow(() -> new IllegalStateException(
						"No instances available for " + serviceId));
		return execute(serviceId, serviceInstance, request);
	}

	@Override
	public <T> T execute(String serviceId, ServiceInstance serviceInstance,
			LoadBalancerRequest<T> request) throws IOException {
		try {
			return request.apply(serviceInstance);
		}
		catch (IOException iOException) {
			throw iOException;
		}
		catch (Exception exception) {
			ReflectionUtils.rethrowRuntimeException(exception);
		}
		return null;
	}

	@Override
	public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return Optional.ofNullable(loadBalancerClientFactory.getInstance(serviceId))
				.map(loadBalancer -> Optional
						.ofNullable(Mono.from(loadBalancer.choose()).block())
						.map(Response::getServer).orElse(null))
				.orElse(null);
	}

}
