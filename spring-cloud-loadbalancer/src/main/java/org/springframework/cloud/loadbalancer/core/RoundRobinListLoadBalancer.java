package org.springframework.cloud.loadbalancer.core;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;

/**
 * @author Olga Maciaszek-Sharma
 */
public class RoundRobinListLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final AtomicInteger position;

	private final ObjectProvider<ServiceInstanceListSupplier<ServiceInstance>> serviceInstanceListSupplier;

	private final String serviceId;

	public RoundRobinListLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceListSupplier<ServiceInstance>> serviceInstanceListSupplier) {
		this(serviceId, serviceInstanceListSupplier, new Random().nextInt(1000));
	}

	public RoundRobinListLoadBalancer(String serviceId,
			ObjectProvider<ServiceInstanceListSupplier<ServiceInstance>> serviceInstanceListSupplier,
			int seedPosition) {
		this.serviceId = serviceId;
		this.serviceInstanceListSupplier = serviceInstanceListSupplier;
		this.position = new AtomicInteger(seedPosition);
	}

	@Override
	// see original
	// https://github.com/Netflix/ocelli/blob/master/ocelli-core/
	// src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
	public Mono<Response<ServiceInstance>> choose(Request request) {
		// TODO: move supplier to Request?
		ServiceInstanceListSupplier<ServiceInstance> supplier = this.serviceInstanceListSupplier.getIfAvailable();
		return supplier.get()
				.next()
				.map(instances -> {
			if (instances.isEmpty()) {
				log.warn("No servers available for service: " + this.serviceId);
				return new EmptyResponse();
			}
			// TODO: enforce order?
			int pos = Math.abs(this.position.incrementAndGet());

			ServiceInstance instance = instances.get(pos % instances.size());

			return new DefaultResponse(instance);
		});
	}
}
