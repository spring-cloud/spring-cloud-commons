package org.springframework.cloud.client.discovery.simple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DiscoveryClientAutoConfigurationDefaultTests {

	@Autowired
	private SimpleDiscoveryProperties props;

	@Autowired
	private DiscoveryClient discoveryClient;

	@Test
	public void noPropsMapping() {
		assertThat(discoveryClient).isInstanceOf(SimpleDiscoveryClient.class);
		List<ServiceInstance> serviceInstances = discoveryClient.getInstances("service1");
		assertThat(serviceInstances.size()).isEqualTo(0);
	}

	@Configuration
	@EnableAutoConfiguration
	public static class SampleConfig {
	}
}
