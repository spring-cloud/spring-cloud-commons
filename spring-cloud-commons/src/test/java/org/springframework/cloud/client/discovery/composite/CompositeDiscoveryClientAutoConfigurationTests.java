package org.springframework.cloud.client.discovery.composite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composite Discovery Client should be the one found by default.
 * 
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class CompositeDiscoveryClientAutoConfigurationTests {

	@Autowired
	private DiscoveryClient discoveryClient;

	@Test
	public void compositeDiscoveryClientShouldBeTheDefault() {
		assertThat(discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);
		CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) discoveryClient;
		assertThat(compositeDiscoveryClient.getDiscoveryClients()).hasSize(2);
		assertThat(compositeDiscoveryClient.getDiscoveryClients().get(0).description())
				.isEqualTo("A custom discovery client");
	}

	@Test
	public void simpleDiscoveryClientShouldBeHaveTheLowestPrecedence() {
		CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) discoveryClient;
		assertThat(compositeDiscoveryClient.getDiscoveryClients().get(0).description())
				.isEqualTo("A custom discovery client");
		assertThat(compositeDiscoveryClient.getDiscoveryClients().get(1))
				.isInstanceOf(SimpleDiscoveryClient.class);
	}

	@EnableAutoConfiguration
	@Configuration
	public static class Config {

		@Bean
		public DiscoveryClient customDiscoveryClient1() {
			return new DiscoveryClient() {
				@Override
				public String description() {
					return "A custom discovery client";
				}

				@Override
				public List<ServiceInstance> getInstances(String serviceId) {
					return null;
				}

				@Override
				public List<String> getServices() {
					return null;
				}
			};
		}
	}
}
