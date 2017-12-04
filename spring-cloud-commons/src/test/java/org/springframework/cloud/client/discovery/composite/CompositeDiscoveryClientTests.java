package org.springframework.cloud.client.discovery.composite;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for behavior of Composite Discovery Client
 * 
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
		"spring.application.name=service0",
		"spring.cloud.discovery.client.simple.instances.service1[0].uri=http://s1-1:8080",
		"spring.cloud.discovery.client.simple.instances.service1[1].uri=https://s1-2:8443",
		"spring.cloud.discovery.client.simple.instances.service2[0].uri=https://s2-1:8080",
		"spring.cloud.discovery.client.simple.instances.service2[1].uri=https://s2-2:443" })
public class CompositeDiscoveryClientTests {

	@Autowired
	private DiscoveryClient discoveryClient;

	@Test
	public void getInstancesByServiceIdShouldDelegateCall() {
		assertThat(this.discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);

		assertThat(this.discoveryClient.getInstances("service1")).hasSize(2);

		ServiceInstance s1 = this.discoveryClient.getInstances("service1").get(0);
		assertThat(s1.getHost()).isEqualTo("s1-1");
		assertThat(s1.getPort()).isEqualTo(8080);
		assertThat(s1.getUri()).isEqualTo(URI.create("http://s1-1:8080"));
		assertThat(s1.isSecure()).isEqualTo(false);
	}
	
	@Test
	public void getServicesShouldAggregateAllServiceNames() {
		assertThat(this.discoveryClient.getServices()).containsOnlyOnce("service1", "service2", "custom");
	}
	
	@Test
	public void getDescriptionShouldBeComposite() {
		assertThat(this.discoveryClient.description()).isEqualTo("Composite Discovery Client");
	}
	
	@Test
	public void getInstancesShouldRespectOrder() {
		assertThat(this.discoveryClient.getInstances("custom")).hasSize(1);
		assertThat(this.discoveryClient.getInstances("custom")).hasSize(1);
	}

	@Test
	public void getInstancesByUnknownServiceIdShouldReturnAnEmptyList() {
		assertThat(this.discoveryClient.getInstances("unknown")).hasSize(0);
	}

	@EnableAutoConfiguration
	@Configuration
	public static class Config {

		@Bean
		@Order(1)
		public DiscoveryClient customDiscoveryClient() {
			return new DiscoveryClient() {
				@Override
				public String description() {
					return "A custom discovery client";
				}

				@Override
				public List<ServiceInstance> getInstances(String serviceId) {
					if (serviceId.equals("custom")) {
						ServiceInstance s1 = new DefaultServiceInstance("custom", "host",
								123, false);
						return Arrays.asList(s1);
					}
					return Collections.emptyList();
				}

				@Override
				public List<String> getServices() {
					return Arrays.asList("custom");
				}
			};
		}
	}
}
