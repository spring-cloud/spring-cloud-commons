package org.springframework.cloud.client.discovery.simple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DiscoveryClient implementation defaults to {@link SimpleDiscoveryClient}
 * @author Biju Kunjummen
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DiscoveryClientAutoConfigurationDefaultTests.Config.class)
public class DiscoveryClientAutoConfigurationDefaultTests {

	@Autowired
	private List<DiscoveryClient> discoveryClients;

	@Test
	public void simpleDiscoveryClientShouldBeTheDefault() {
		assertThat(discoveryClients).hasSize(2);
		assertThat(discoveryClients.get(0) instanceof SimpleDiscoveryClient
				|| discoveryClients.get(0) instanceof CompositeDiscoveryClient);
		
		assertThat(discoveryClients.get(1) instanceof SimpleDiscoveryClient
				|| discoveryClients.get(1) instanceof CompositeDiscoveryClient);
	}

	@EnableAutoConfiguration
	@Configuration
	public static class Config {
	}
}
