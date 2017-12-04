package org.springframework.cloud.client.discovery.simple;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for mapping properties to instances in {@link SimpleDiscoveryClient}
 *
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.application.name=service0",
		"spring.cloud.discovery.client.simple.instances.service1[0].uri=http://s1-1:8080",
		"spring.cloud.discovery.client.simple.instances.service1[1].uri=https://s1-2:8443",
		"spring.cloud.discovery.client.simple.instances.service2[0].uri=https://s2-1:8080",
		"spring.cloud.discovery.client.simple.instances.service2[1].uri=https://s2-2:443" })
public class SimpleDiscoveryClientPropertiesMappingTests {

	@Autowired
	private SimpleDiscoveryProperties props;

	@Autowired
	private SimpleDiscoveryClient discoveryClient;

	@Test
	public void propsShouldGetCleanlyMapped() {
		assertThat(this.props.getInstances().size()).isEqualTo(2);
		assertThat(this.props.getInstances().get("service1").size()).isEqualTo(2);
		assertThat(this.props.getInstances().get("service1").get(0).getHost())
				.isEqualTo("s1-1");
		assertThat(this.props.getInstances().get("service1").get(0).getPort())
				.isEqualTo(8080);
		assertThat(this.props.getInstances().get("service1").get(0).getUri())
				.isEqualTo(URI.create("http://s1-1:8080"));
		assertThat(this.props.getInstances().get("service1").get(0).isSecure())
				.isEqualTo(false);

		assertThat(this.props.getInstances().get("service2").size()).isEqualTo(2);
		assertThat(this.props.getInstances().get("service2").get(0).getHost())
				.isEqualTo("s2-1");
		assertThat(this.props.getInstances().get("service2").get(0).getPort())
				.isEqualTo(8080);
		assertThat(this.props.getInstances().get("service2").get(0).getUri())
				.isEqualTo(URI.create("https://s2-1:8080"));
		assertThat(this.props.getInstances().get("service2").get(0).isSecure())
				.isEqualTo(true);
	}

	@Test
	public void testDiscoveryClientShouldResolveSimpleValues() {
		assertThat(this.discoveryClient.description())
				.isEqualTo("Simple Discovery Client");
		assertThat(this.discoveryClient.getInstances("service1")).hasSize(2);

		ServiceInstance s1 = this.discoveryClient.getInstances("service1").get(0);
		assertThat(s1.getHost()).isEqualTo("s1-1");
		assertThat(s1.getPort()).isEqualTo(8080);
		assertThat(s1.getUri()).isEqualTo(URI.create("http://s1-1:8080"));
		assertThat(s1.isSecure()).isEqualTo(false);
	}

	@Test
	public void testGetServices() {
		assertThat(this.discoveryClient.getServices())
				.containsExactlyInAnyOrder("service1", "service2");
	}

	@Test
	public void testGetANonExistentServiceShouldReturnAnEmptyList() {
		assertThat(this.discoveryClient.getInstances("nonexistent")).isNotNull();
		assertThat(this.discoveryClient.getInstances("nonexistent")).isEmpty();
	}

	@Configuration
	@EnableAutoConfiguration
	public static class SampleConfig {
	}
}
