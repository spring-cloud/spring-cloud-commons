package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ZonePreferenceServiceInstanceListSupplier implements ServiceInstanceListSupplier {

	private final ServiceInstanceListSupplier delegate;

	private final Environment environment;

	private String zone;

	public ZonePreferenceServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, Environment environment) {
		this.delegate = delegate;
		this.environment = environment;
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get()
				.map(this::filteredByZone);
	}

	private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
		if (zone == null) {
			zone = environment.getProperty("spring.cloud.loadbalancer.zone");
		}
		if (zone != null) {
			List<ServiceInstance> filteredInstances = serviceInstances.stream()
					.filter(serviceInstance -> serviceInstance.getZone() != null)
					.filter(serviceInstance -> zone
							.equalsIgnoreCase(serviceInstance.getZone()))
					.collect(Collectors.toList());
			if (filteredInstances.size() > 0) {
				return filteredInstances;
			}
		}
		return serviceInstances;
	}

}
