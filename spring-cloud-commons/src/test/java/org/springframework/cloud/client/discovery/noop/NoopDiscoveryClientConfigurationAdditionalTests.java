package org.springframework.cloud.client.discovery.noop;

import java.util.List;

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

import static org.junit.Assert.assertFalse;

/**
 * Tests if @EnableDiscoveryClient is NOT used, then NoopDiscoveryClient is created.
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NoopDiscoveryClientConfigurationAdditionalTests.App.class)
public class NoopDiscoveryClientConfigurationAdditionalTests {

	@Autowired
	DiscoveryClient discoveryClient;

	@Test
	public void testDiscoveryClientIsNotNoop() {
		assertFalse("discoveryClient is wrong instance type", discoveryClient instanceof NoopDiscoveryClient);
	}

	@EnableAutoConfiguration
	@Configuration
	public static class App {

		@Bean
		public DiscoveryClient discoveryClient() {
			return new DiscoveryClient() {
				@Override
				public String description() {
					return null;
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
