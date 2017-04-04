package org.springframework.cloud.client.discovery.simple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DiscoveryClient implementation defaults to {@link SimpleDiscoveryClient}
 * @author Biju Kunjummen
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DiscoveryClientAutoConfigurationDefaultTests.App.class)
public class DiscoveryClientAutoConfigurationDefaultTests {

	@Autowired
	DiscoveryClient discoveryClient;

	@Test
	public void simpleDiscoveryClientShouldBeTheDefault() {
		assertThat(discoveryClient).isInstanceOf(SimpleDiscoveryClient.class);
	}

	@EnableAutoConfiguration
	@Configuration
	public static class App {
		public static void main(String[] args) {
			SpringApplication.run(App.class, args);
		}
	}
}
