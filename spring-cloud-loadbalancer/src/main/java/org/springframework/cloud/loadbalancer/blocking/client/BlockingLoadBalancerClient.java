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

package org.springframework.cloud.loadbalancer.blocking.client;

import java.io.IOException;
import java.net.URI;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext;
import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext.Status;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.util.ReflectionUtils;

/**
 * The default {@link LoadBalancerClient} implementation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class BlockingLoadBalancerClient implements LoadBalancerClient {

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	public BlockingLoadBalancerClient(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request)
			throws IOException {
		Response<ServiceInstance> response = chooseResponse(serviceId);
		if (response == null || !response.hasServer()) {
			response.onComplete(new CompletionContext(Status.DISCARD));
			throw new IllegalStateException("No instances available for " + serviceId);
		}
		return execute(serviceId, response, request);
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

	public <T> T execute(String serviceId, Response<ServiceInstance> response,
			LoadBalancerRequest<T> request) throws IOException {
		T retVal = null;
		try {
			retVal = request.apply(response.getServer());
			response.onComplete(new CompletionContext(Status.SUCCESSS));
		}
		catch (IOException iOException) {
			response.onComplete(new CompletionContext(Status.FAILED, iOException));
			throw iOException;
		}
		catch (Exception exception) {
			response.onComplete(new CompletionContext(Status.FAILED, exception));
			ReflectionUtils.rethrowRuntimeException(exception);
		}
		return retVal;
	}

	@Override
	public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		Response<ServiceInstance> loadBalancerResponse = chooseResponse(serviceId);
		if (loadBalancerResponse == null) return null;
		return loadBalancerResponse.getServer();
	}

	private Response<ServiceInstance> chooseResponse(String serviceId) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory
				.getInstance(serviceId);
		if (loadBalancer == null) {
			return null;
		}
		Response<ServiceInstance> loadBalancerResponse = Mono.from(loadBalancer.choose())
				.block();
		if (loadBalancerResponse == null) {
			return null;
		}
		return loadBalancerResponse;
	}

}
