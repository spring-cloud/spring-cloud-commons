package org.springframework.cloud.loadbalancer.core;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;

/**
 * @author Olga Maciaszek-Sharma
 */
public class PowerOfTwoChoicesPOCLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final ObjectProvider<ServiceInstanceListSupplier<ConnectionTrackingServiceInstance>> serviceInstanceListSupplier;

	private final String serviceId;

	private List<ConnectionTrackingServiceInstance> instances;

	public PowerOfTwoChoicesPOCLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceListSupplier<ConnectionTrackingServiceInstance>> serviceInstanceListSupplier) {
		this.serviceId = serviceId;
		this.serviceInstanceListSupplier = serviceInstanceListSupplier;
		resetInstances();
	}

	private void resetInstances() {
		Schedulers.fromExecutorService(Executors.newSingleThreadScheduledExecutor())
				.schedulePeriodically(() -> instances = serviceInstanceListSupplier
						// maybe we don't have to block at all?
						// TODO:  sensible interval defaults + config
						.getIfAvailable().get().block(), 0, 10, TimeUnit.MINUTES);
	}

	// TODO: optimise
	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		if (instances.isEmpty()) {
			log.warn("No servers available for service: " + this.serviceId);
			return Mono.just(new EmptyResponse());
		}
		if (instances.size() == 1 || instances.get(0).getConnectionCount() < instances
				.get(1)
				.getConnectionCount()) {
			instances.get(0).addConnection();
			return Mono.just(new DefaultResponse(instances.get(0)));
		}
		Collections.shuffle(instances);

		instances.get(1).addConnection();
		return Mono.just(new DefaultResponse(instances.get(1)));
	}

}
