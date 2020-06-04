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

package org.springframework.cloud.loadbalancer.blocking.client;

import java.io.IOException;
import java.net.URI;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedCallExecution;
import org.springframework.cloud.client.loadbalancer.reactive.NoOpLoadBalancedCallExecutionCallback;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
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

	private final LoadBalancedCallExecution.Callback<DefaultRequestContext, ServiceInstance> callback;

	public BlockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory,
			LoadBalancedCallExecution.Callback<DefaultRequestContext, ServiceInstance> callback) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.callback = callback;
	}

	public BlockingLoadBalancerClient(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		this(loadBalancerClientFactory, new NoOpLoadBalancedCallExecutionCallback<>());
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory
				.getInstance(serviceId);
		if (loadBalancer == null) {
			return null;
		}
		ReactiveLoadBalancer.RequestExecution<T, DefaultRequestContext, ServiceInstance> requestExecution = new ReactiveLoadBalancer.RequestExecution<T, DefaultRequestContext, ServiceInstance>() {
			@Override
			public Publisher<T> apply(Response<ServiceInstance> response) {
				if (response.hasServer()) {
					try {
						return Mono.just(request.apply(response.getServer()));
					}
					catch (Exception e) {
						return Mono.error(e);
					}
				}
				if (!response.hasServer()) {
					throw new IllegalStateException(
							"No instances available for " + serviceId);
				}
				return Mono.empty();
			}

			@Override
			public Request<DefaultRequestContext> createRequest() {
				return new DefaultRequest<>(new DefaultRequestContext(request));
			}
		};
		return Mono.from(loadBalancer.execute(requestExecution, callback)).block();
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
		Response<ServiceInstance> response = chooseResponse(serviceId);
		if (!response.hasServer()) {
			return null;
		}
		return response.getServer();
	}

	private Response<ServiceInstance> chooseResponse(String serviceId) {
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory
				.getInstance(serviceId);
		if (loadBalancer == null) {
			return null;
		}
		Response<ServiceInstance> response = Mono.from(loadBalancer.choose()).block();
		if (response == null) {
			return new EmptyResponse();
		}
		return response;
	}

}
