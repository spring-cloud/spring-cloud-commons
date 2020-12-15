package org.springframework.cloud.loadbalancer.stats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import org.springframework.boot.actuate.metrics.http.Outcome;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * @author Olga Maciaszek-Sharma
 */
public class MicrometerStatsLifecycle implements LoadBalancerLifecycle<RequestDataContext, ResponseData, ServiceInstance> {

	private final ConcurrentHashMap<ServiceInstance, AtomicLong> activeRequestsPerInstance = new ConcurrentHashMap<>();

	private final MeterRegistry meterRegistry;

	public MicrometerStatsLifecycle(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	private static Tag status(HttpStatus status) {
		if (status == null) {
			status = HttpStatus.OK;
		}
		return Tag.of("status", String.valueOf(status.value()));
	}

	private static Tag exception(Throwable exception) {
		if (exception != null) {
			String simpleName = exception.getClass().getSimpleName();
			return Tag.of("exception", StringUtils
					.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}
		return Tag.of("exception", "None");
	}


	@Override
	public boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public void onStart(Request<RequestDataContext> request) {

	}

	@Override
	public void onStartRequest(Request<RequestDataContext> request, Response<ServiceInstance> lbResponse) {
		request.getContext().setRequestStartTime(System.nanoTime());
		if (!lbResponse.hasServer()) {
			return;
		}
		ServiceInstance serviceInstance = lbResponse.getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.get(serviceInstance);
		if (activeRequestsCounter == null) {
			activeRequestsPerInstance.put(serviceInstance, new AtomicLong());
			AtomicLong createdCounter = activeRequestsPerInstance.get(serviceInstance);
			Gauge.builder("loadbalanced.requests.active", () -> createdCounter)
					.tags(Tags.of(Tag.of("serviceId", serviceInstance.getServiceId()),
							Tag.of("instanceId", serviceInstance.getInstanceId())))
					.register(meterRegistry);
			createdCounter.incrementAndGet();
		}
	}

	@Override
	public void onComplete(CompletionContext<ResponseData, ServiceInstance, RequestDataContext> completionContext) {
		long requestFinishedTimestamp = System.nanoTime();
		if (CompletionContext.Status.DISCARD.equals(completionContext.status())) {
			return;
		}
		ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse()
				.getServer();
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.get(serviceInstance);
		if (activeRequestsCounter != null) {
			activeRequestsCounter.decrementAndGet();
		}
		Request<RequestDataContext> loadBalancerRequest = completionContext
				.getLoadBalancerRequest();
		RequestData requestData = loadBalancerRequest.getContext()
				.getClientRequest();
		ResponseData responseData = completionContext.getClientResponse();
		Timer.builder("loadbalanced.requests.total")
				// Tags corresponding to the ones used in actuator
				.tags(Tags.of(Tag.of("lbCompletionStatus", completionContext.status()
								.toString()),
						Tag.of("method", requestData.getHttpMethod().toString()),
						Tag.of("uri", requestData.getUrl().getPath()),
						exception(completionContext.getThrowable()),
						Outcome.forStatus(responseData.getHttpStatus().value()).asTag(),
						status(responseData.getHttpStatus())
				))
				.register(meterRegistry)
				.record(requestFinishedTimestamp - loadBalancerRequest.getContext()
								.getRequestStartTimestamp(),
						TimeUnit.NANOSECONDS);
	}

}
