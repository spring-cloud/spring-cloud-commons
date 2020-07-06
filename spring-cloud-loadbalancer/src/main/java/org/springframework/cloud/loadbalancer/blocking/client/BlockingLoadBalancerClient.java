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
import java.util.Set;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
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

	private final Set<LoadBalancerLifecycle> supportedLifecycleProcessors;

	private final LoadBalancerProperties properties;

	public BlockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory,
			Set<LoadBalancerLifecycle> lifecycleProcessors,
			LoadBalancerProperties properties) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.supportedLifecycleProcessors = lifecycleProcessors.stream()
				.filter(lifecycle -> lifecycle.supports(DefaultRequestContext.class,
						Object.class, ServiceInstance.class))
				.collect(Collectors.toSet());
		this.properties = properties;
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request)
			throws IOException {
		supportedLifecycleProcessors
				.forEach(lifecycle -> lifecycle.onStart(new DefaultRequest<>(
						new DefaultRequestContext(request, properties.getHint()))));
		ServiceInstance serviceInstance = choose(serviceId);
		if (serviceInstance == null) {
			supportedLifecycleProcessors
					.forEach(lifecycle -> lifecycle.onComplete(new CompletionContext<>(
							CompletionContext.Status.DISCARD, new EmptyResponse())));
			throw new IllegalStateException("No instances available for " + serviceId);
		}
		return execute(serviceId, serviceInstance, request);
	}

	@Override
	public <T> T execute(String serviceId, ServiceInstance serviceInstance,
			LoadBalancerRequest<T> request) throws IOException {
		DefaultResponse defaultResponse = new DefaultResponse(serviceInstance);
		try {
			T response = request.apply(serviceInstance);
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
					.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS,
							defaultResponse, response)));
			return response;
		}
		catch (IOException iOException) {
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
					.onComplete(new CompletionContext<>(CompletionContext.Status.FAILED,
							iOException, defaultResponse)));
			throw iOException;
		}
		catch (Exception exception) {
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
					.onComplete(new CompletionContext<>(CompletionContext.Status.FAILED,
							exception, defaultResponse)));
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
		return loadBalancerResponse.getServer();
	}

}
