package org.springframework.cloud.client.discovery.simple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Biju Kunjummen
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = UserDefinedDiscoveryClientOverridesDefaultsTests.App.class)
public class UserDefinedDiscoveryClientOverridesDefaultsTests {

	@Autowired
	DiscoveryClient discoveryClient;

	@Test
	public void testDiscoveryClientIsNotNoop() {
		assertThat(discoveryClient).isNotInstanceOf(SimpleDiscoveryClient.class);

		assertThat(discoveryClient.description())
				.isEqualTo("user defined discovery client");
	}

	@EnableAutoConfiguration
	@Configuration
	public static class App {

		@Bean
		public DiscoveryClient discoveryClient() {
			return new DiscoveryClient() {
				@Override
				public String description() {
					return "user defined discovery client";
				}

				@Override
				public ServiceInstance getLocalServiceInstance() {
					return null;
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

		public static void main(String[] args) {
			SpringApplication.run(App.class, args);
		}
	}
}
