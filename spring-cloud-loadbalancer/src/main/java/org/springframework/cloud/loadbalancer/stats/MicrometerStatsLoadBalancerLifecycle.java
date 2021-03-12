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
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.TimedRequestContext;

import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildDiscardedRequestTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildFailedRequestTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildServiceInstanceTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildSuccessRequestTags;

/**
 * An implementation of {@link LoadBalancerLifecycle} that records metrics for
 * load-balanced calls.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class MicrometerStatsLoadBalancerLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {

	private final MeterRegistry meterRegistry;

	private final ConcurrentHashMap<ServiceInstance, AtomicLong> activeRequestsPerInstance = new ConcurrentHashMap<>();

	public MicrometerStatsLoadBalancerLifecycle(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
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
		if (request.getContext() instanceof TimedRequestContext) {
			((TimedRequestContext) request.getContext()).setRequestStartTime(System.nanoTime());
		}
		if (!lbResponse.hasServer()) {
			return;
		}
		ServiceInstance serviceInstance = lbResponse.getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.computeIfAbsent(serviceInstance, instance -> {
			AtomicLong createdCounter = new AtomicLong();
			Gauge.builder("loadbalancer.requests.active", () -> createdCounter)
					.tags(buildServiceInstanceTags(serviceInstance)).register(meterRegistry);
			return createdCounter;
		});
		activeRequestsCounter.incrementAndGet();
	}

	@Override
	public void onComplete(CompletionContext<Object, ServiceInstance, Object> completionContext) {
		long requestFinishedTimestamp = System.nanoTime();
		if (CompletionContext.Status.DISCARD.equals(completionContext.status())) {
			Counter.builder("loadbalancer.requests.discard").tags(buildDiscardedRequestTags(completionContext))
					.register(meterRegistry).increment();
			return;
		}
		ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse().getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.get(serviceInstance);
		if (activeRequestsCounter != null) {
			activeRequestsCounter.decrementAndGet();
		}
		Object loadBalancerRequestContext = completionContext.getLoadBalancerRequest().getContext();
		if (requestHasBeenTimed(loadBalancerRequestContext)) {
			if (CompletionContext.Status.FAILED.equals(completionContext.status())) {
				Timer.builder("loadbalancer.requests.failed").tags(buildFailedRequestTags(completionContext))
						.register(meterRegistry)
						.record(requestFinishedTimestamp
								- ((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime(),
								TimeUnit.NANOSECONDS);
				return;
			}
			Timer.builder("loadbalancer.requests.success").tags(buildSuccessRequestTags(completionContext))
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
