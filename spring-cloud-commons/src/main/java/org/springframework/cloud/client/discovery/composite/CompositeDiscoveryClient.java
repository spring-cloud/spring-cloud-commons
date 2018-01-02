package org.springframework.cloud.client.discovery.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * A {@link DiscoveryClient} composed of other Discovery Client's and will delegate the
 * calls to each of them in order
 * 
 * @author Biju Kunjummen
 */
public class CompositeDiscoveryClient implements DiscoveryClient {

	private final List<DiscoveryClient> discoveryClients;

	public CompositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
		this.discoveryClients = discoveryClients;
	}

	@Override
	public String description() {
		return "Composite Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : discoveryClients) {
				List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
				if (instances != null && !instances.isEmpty()) {
					return instances;
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getServices() {
		LinkedHashSet<String> services = new LinkedHashSet<>();
		if (this.discoveryClients != null) {
			for (DiscoveryClient discoveryClient : discoveryClients) {
				List<String> serviceForClient = discoveryClient.getServices();
				if (serviceForClient != null) {
					services.addAll(serviceForClient);
				}
			}
		}
		return new ArrayList<>(services);
	}

	public List<DiscoveryClient> getDiscoveryClients() {
		return discoveryClients;
	}
}
