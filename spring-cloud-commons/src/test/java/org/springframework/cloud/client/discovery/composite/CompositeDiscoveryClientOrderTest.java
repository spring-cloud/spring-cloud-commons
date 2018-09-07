package org.springframework.cloud.client.discovery.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.CUSTOM_DISCOVERY_CLIENT;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.CUSTOM_SERVICE_ID;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.DEFAULT_ORDER_DISCOVERY_CLIENT;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.FOURTH_DISCOVERY_CLIENT;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests for the support of ordered {@link DiscoveryClient} instances in {@link CompositeDiscoveryClient}
 *
 * @author Olga Maciaszek-Sharma
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.discovery.client.simple.order:2", classes = {
		CompositeDiscoveryClientTestsConfig.class })
public class CompositeDiscoveryClientOrderTest {

	@Autowired
	DiscoveryClient discoveryClient;

	@Test
	public void shouldGetOrderedDiscoveryClients() {
		// when:
		List<DiscoveryClient> discoveryClients = ((CompositeDiscoveryClient) this.discoveryClient)
				.getDiscoveryClients();

		// then:
		assertThat(discoveryClients.get(0).description())
				.isEqualTo(CUSTOM_DISCOVERY_CLIENT);
		assertThat(discoveryClients.get(1).description())
				.isEqualTo(DEFAULT_ORDER_DISCOVERY_CLIENT);
		assertThat(discoveryClients.get(2).description())
				.isEqualTo("Simple Discovery Client");
		assertThat(discoveryClients.get(3).description())
				.isEqualTo(FOURTH_DISCOVERY_CLIENT);
	}

	@Test
	public void shouldOnlyReturnServiceInstancesForTheHighestPrecedenceDiscoveryClient() {
		// when:
		List<ServiceInstance> serviceInstances = this.discoveryClient
				.getInstances(CUSTOM_SERVICE_ID);

		// then:
		assertThat(serviceInstances).hasSize(1);
		assertThat(serviceInstances.get(0).getPort()).isEqualTo(123);
	}
}
