package org.springframework.cloud.client.discovery.composite;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration for {@link CompositeDiscoveryClient} tests.
 *
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@EnableAutoConfiguration
public class CompositeDiscoveryClientTestsConfig {

	static final String DEFAULT_ORDER_DISCOVERY_CLIENT = "Default order discovery client";
	static final String CUSTOM_DISCOVERY_CLIENT = "A custom discovery client";
	static final String FOURTH_DISCOVERY_CLIENT = "Fourth discovery client";
	static final String CUSTOM_SERVICE_ID = "custom";

	@Bean
	public DiscoveryClient customDiscoveryClient() {
		return aDiscoveryClient(-1, CUSTOM_DISCOVERY_CLIENT);
	}

	@Bean
	public DiscoveryClient thirdOrderCustomDiscoveryClient() {
		return aDiscoveryClient(3, FOURTH_DISCOVERY_CLIENT);
	}

	@Bean
	public DiscoveryClient defaultOrderDiscoveryClient() {
		return aDiscoveryClient(null, DEFAULT_ORDER_DISCOVERY_CLIENT);
	}

	private DiscoveryClient aDiscoveryClient(Integer order, String description) {
		return new DiscoveryClient() {
			@Override
			public String description() {
				return description;
			}

			@Override
			public List<ServiceInstance> getInstances(String serviceId) {
				if (serviceId.equals(CUSTOM_SERVICE_ID)) {
					ServiceInstance s1 = new DefaultServiceInstance(CUSTOM_SERVICE_ID,
							"host", 123, false);
					return singletonList(s1);
				}
				return Collections.emptyList();
			}

			@Override
			public List<String> getServices() {
				return singletonList(CUSTOM_SERVICE_ID);
			}

			@Override
			public int getOrder() {
				return order != null ? order : DiscoveryClient.super.getOrder();
			}
		};
	}
}
