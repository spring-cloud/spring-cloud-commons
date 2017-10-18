package org.springframework.cloud.client.discovery.simple;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties.SimpleServiceInstance;

/**
 * A {@link org.springframework.cloud.client.discovery.DiscoveryClient} that will use the
 * properties file as a source of service instances
 *
 * @author Biju Kunjummen
 */
public class SimpleDiscoveryClient implements DiscoveryClient {

	private SimpleDiscoveryProperties simpleDiscoveryProperties;

	public SimpleDiscoveryClient(SimpleDiscoveryProperties simpleDiscoveryProperties) {
		this.simpleDiscoveryProperties = simpleDiscoveryProperties;
	}

	@Override
	public String description() {
		return "Simple Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		List<SimpleServiceInstance> serviceInstanceForService = this.simpleDiscoveryProperties
				.getInstances().get(serviceId);

		if (serviceInstanceForService != null) {
			serviceInstances.addAll(serviceInstanceForService);
		}
		return serviceInstances;
	}

	@Override
	public List<String> getServices() {
		return new ArrayList<>(this.simpleDiscoveryProperties.getInstances().keySet());
	}
}
