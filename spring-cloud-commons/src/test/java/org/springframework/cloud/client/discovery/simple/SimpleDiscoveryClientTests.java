package org.springframework.cloud.client.discovery.simple;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleDiscoveryClientTests {

	private SimpleDiscoveryClient simpleDiscoveryClient;

	@Before
	public void setUp() {
		SimpleDiscoveryProperties simpleDiscoveryProperties = new SimpleDiscoveryProperties();

		Map<String, List<SimpleServiceInstance>> map = new HashMap<>();
		SimpleServiceInstance service1Inst1 = new SimpleServiceInstance(
				"http://host1:8080");
		SimpleServiceInstance service1Inst2 = new SimpleServiceInstance(
				"https://host2:8443");
		map.put("service1", Arrays.asList(service1Inst1, service1Inst2));
		simpleDiscoveryProperties.setInstances(map);
		this.simpleDiscoveryClient = new SimpleDiscoveryClient(simpleDiscoveryProperties);
	}

	@Test
	public void shouldBeAbleToRetrieveServiceDetailsByName() {
		List<ServiceInstance> instances = this.simpleDiscoveryClient
				.getInstances("service1");
		assertThat(instances.size()).isEqualTo(2);
		assertThat(instances.get(0).getHost()).isEqualTo("host1");
		assertThat(instances.get(0).getPort()).isEqualTo(8080);
		assertThat(instances.get(0).getUri()).isEqualTo(URI.create("http://host1:8080"));
		assertThat(instances.get(0).isSecure()).isEqualTo(false);
	}
}
