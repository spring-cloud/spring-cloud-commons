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

import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildLoadBalancedRequestTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildServiceInstanceTags;

/**
 * @author Olga Maciaszek-Sharma
 */
public class MicrometerStatsLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {

	private final MeterRegistry meterRegistry;
	private final ConcurrentHashMap<ServiceInstance, AtomicLong> activeRequestsPerInstance = new ConcurrentHashMap<>();

	public MicrometerStatsLifecycle(MeterRegistry meterRegistry) {
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
			((TimedRequestContext) request.getContext())
					.setRequestStartTime(System.nanoTime());
		}
		if (!lbResponse.hasServer()) {
			return;
		}
		ServiceInstance serviceInstance = lbResponse.getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance
				.computeIfAbsent(serviceInstance,
						instance -> {
							AtomicLong createdCounter = activeRequestsPerInstance
									.get(serviceInstance);
							Gauge.builder("loadbalanced.requests.active", () -> createdCounter)
									.tags(buildServiceInstanceTags(serviceInstance))
									.register(meterRegistry);
							return createdCounter;
						});
		activeRequestsCounter.incrementAndGet();
	}

	@Override
	public void onComplete(CompletionContext<Object, ServiceInstance, Object> completionContext) {
		long requestFinishedTimestamp = System.nanoTime();
		if (CompletionContext.Status.DISCARD.equals(completionContext.status())) {
			Counter.builder("loadbalanced.requests.discarded")
					.tags(buildLoadBalancedRequestTags(completionContext))
					.register(meterRegistry)
					.increment();
			return;
		}
		ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse()
				.getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.get(serviceInstance);
		if (activeRequestsCounter != null) {
			activeRequestsCounter.decrementAndGet();
		}
		Object loadBalancerRequestContext = completionContext.getLoadBalancerRequest()
				.getContext();
		if (loadBalancerRequestContext instanceof TimedRequestContext) {
			Timer.builder("loadbalanced.requests.executed")
					.tags(buildLoadBalancedRequestTags(completionContext)
					)
					.register(meterRegistry)
					.record(requestFinishedTimestamp - ((TimedRequestContext) loadBalancerRequestContext)
							.getRequestStartTime(), TimeUnit.NANOSECONDS);
		}
	}

}
