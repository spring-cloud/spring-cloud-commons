/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.stats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.TimedRequestContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildServiceInstanceTags;

/**
 * An implementation of {@link LoadBalancerLifecycle} that records metrics for
 * load-balanced calls.
 *
 * @author Olga Maciaszek-Sharma
 * @author Jaroslaw Dembek
 * @since 3.0.0
 */
public class MicrometerStatsLoadBalancerLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {

	private final MeterRegistry meterRegistry;

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	private final ConcurrentHashMap<ServiceInstance, AtomicLong> activeRequestsPerInstance = new ConcurrentHashMap<>();

	public MicrometerStatsLoadBalancerLifecycle(MeterRegistry meterRegistry,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
		this.meterRegistry = meterRegistry;
		this.loadBalancerFactory = loadBalancerFactory;
	}

	/**
	 * Creates a MicrometerStatsLoadBalancerLifecycle instance based on the provided
	 * {@link MeterRegistry}.
	 * @param meterRegistry {@link MeterRegistry} to use for Micrometer metrics.
	 * @deprecated in favour of
	 * {@link MicrometerStatsLoadBalancerLifecycle#MicrometerStatsLoadBalancerLifecycle(MeterRegistry, ReactiveLoadBalancer.Factory)}
	 */
	@Deprecated(forRemoval = true)
	public MicrometerStatsLoadBalancerLifecycle(MeterRegistry meterRegistry) {
		// use default properties when calling deprecated constructor
		this(meterRegistry, new LoadBalancerClientFactory(new LoadBalancerClientsProperties()));
	}

	@Override
	public boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
		return ServiceInstance.class.isAssignableFrom(serverTypeClass);
	}

	@Override
	public void onStart(Request<Object> request) {
		// do nothing
	}

	@Override
	public void onStartRequest(Request<Object> request, Response<ServiceInstance> lbResponse) {
		if (request != null && request.getContext() instanceof TimedRequestContext) {
			((TimedRequestContext) request.getContext()).setRequestStartTime(System.nanoTime());
		}
		if (lbResponse == null || !lbResponse.hasServer()) {
			return;
		}
		ServiceInstance serviceInstance = lbResponse.getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.computeIfAbsent(serviceInstance, instance -> {
			AtomicLong createdCounter = new AtomicLong();
			Gauge.builder("loadbalancer.requests.active", () -> createdCounter)
				.tags(buildServiceInstanceTags(serviceInstance))
				.register(meterRegistry);
			return createdCounter;
		});
		activeRequestsCounter.incrementAndGet();
	}

	@Override
	public void onComplete(CompletionContext<Object, ServiceInstance, Object> completionContext) {
		ServiceInstance serviceInstance = null;
		Response<ServiceInstance> loadBalancerResponse = completionContext.getLoadBalancerResponse();
		if (loadBalancerResponse != null) {
			serviceInstance = loadBalancerResponse.getServer();
		}
		LoadBalancerProperties properties = serviceInstance != null
				? loadBalancerFactory.getProperties(serviceInstance.getServiceId())
				: loadBalancerFactory.getProperties(null);
		LoadBalancerTags loadBalancerTags = new LoadBalancerTags(properties);
		long requestFinishedTimestamp = System.nanoTime();
		if (CompletionContext.Status.DISCARD.equals(completionContext.status())) {
			Counter.builder("loadbalancer.requests.discard")
				.tags(loadBalancerTags.buildDiscardedRequestTags(completionContext))
				.register(meterRegistry)
				.increment();
			return;
		}
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.get(serviceInstance);
		if (activeRequestsCounter != null) {
			activeRequestsCounter.decrementAndGet();
		}
		Request<Object> lbRequest = completionContext.getLoadBalancerRequest();
		if (lbRequest == null) {
			return;
		}
		Object loadBalancerRequestContext = lbRequest.getContext();
		if (requestHasBeenTimed(loadBalancerRequestContext)) {
			if (CompletionContext.Status.FAILED.equals(completionContext.status())) {
				Timer.builder("loadbalancer.requests.failed")
					.tags(loadBalancerTags.buildFailedRequestTags(completionContext))
					.register(meterRegistry)
					.record(requestFinishedTimestamp
							- ((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime(),
							TimeUnit.NANOSECONDS);
				return;
			}
			Timer.builder("loadbalancer.requests.success")
				.tags(loadBalancerTags.buildSuccessRequestTags(completionContext))
				.register(meterRegistry)
				.record(requestFinishedTimestamp
						- ((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime(),
						TimeUnit.NANOSECONDS);
		}
	}

	private boolean requestHasBeenTimed(Object loadBalancerRequestContext) {
		return loadBalancerRequestContext instanceof TimedRequestContext
				&& (((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime() != 0L);
	}

}
