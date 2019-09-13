package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * @author Olga Maciaszek-Sharma
 */
public class DiscoveryClientServiceInstanceListSupplier implements ServiceInstanceListSupplier<ConnectionTrackingServiceInstance> {

	private final DiscoveryClient delegate;

	private final String serviceId;

	public DiscoveryClientServiceInstanceListSupplier(DiscoveryClient delegate,
			Environment environment) {
		this.delegate = delegate;
		this.serviceId = environment.getProperty(PROPERTY_NAME);
	}

	@Override
	public Mono<List<ConnectionTrackingServiceInstance>> get() {
		List<ConnectionTrackingServiceInstance> instances = this.delegate
				.getInstances(this.serviceId)
				.stream()
				// switch to a more sensible conversion
				.map(serviceInstance -> (ConnectionTrackingServiceInstance) serviceInstance)
				.collect(Collectors.toList());
		return Mono.just(instances);
	}

	public String getServiceId() {
		return this.serviceId;
	}
}
