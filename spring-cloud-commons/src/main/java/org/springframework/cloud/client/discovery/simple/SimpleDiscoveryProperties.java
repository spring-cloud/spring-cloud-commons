package org.springframework.cloud.client.discovery.simple;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Properties to hold the details of a
 * {@link org.springframework.cloud.client.discovery.DiscoveryClient} service instances
 * for a given service
 *
 * @author Biju Kunjummen
 */

@ConfigurationProperties(prefix = "spring.cloud.discovery.client.simple")
public class SimpleDiscoveryProperties {
	private Map<String, List<SimpleServiceInstance>> instances = new HashMap<>();

	public Map<String, List<SimpleServiceInstance>> getInstances() {
		return instances;
	}

	public void setInstances(Map<String, List<SimpleServiceInstance>> instances) {
		this.instances = instances;
	}

}
