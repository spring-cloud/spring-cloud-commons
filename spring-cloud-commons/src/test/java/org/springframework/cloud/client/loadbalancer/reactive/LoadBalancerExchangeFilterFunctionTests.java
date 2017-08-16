package org.springframework.cloud.client.loadbalancer.reactive;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties.SimpleServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class LoadBalancerExchangeFilterFunctionTests {

	@Autowired
	private LoadBalancerExchangeFilterFunction lbFunction;

	@Autowired
	private SimpleDiscoveryProperties properties;

	@LocalServerPort
	private int port;

	@Before
	public void before() {
		SimpleServiceInstance instance = new SimpleServiceInstance();
		instance.setServiceId("testservice");
		instance.setUri(URI.create("http://localhost:" + this.port));
		properties.getInstances().put("testservice", Arrays.asList(instance));
	}

	@Test
	public void testFilterFunctionWorks() {
		String value = WebClient.builder()
				.baseUrl("http://testservice")
				.filter(lbFunction)
				.build()
				.get()
				.uri("/hello")
				.retrieve()
				.bodyToMono(String.class).block();
		assertThat(value).isEqualTo("Hello World");
	}

	@EnableDiscoveryClient
	@EnableAutoConfiguration
	@SpringBootConfiguration
	@RestController
	static class Config {

		@RequestMapping("/hello")
		public String hello() {
			return "Hello World";
		}

		@Bean
		LoadBalancerClient loadBalancerClient(DiscoveryClient discoveryClient) {
			return new LoadBalancerClient() {
				Random random = new Random();

				@Override
				public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
					throw new UnsupportedOperationException();
				}

				@Override
				public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
					throw new UnsupportedOperationException();
				}

				@Override
				public URI reconstructURI(ServiceInstance instance, URI original) {
					return UriComponentsBuilder.fromUri(original)
							.host(instance.getHost())
							.port(instance.getPort())
							.build()
							.toUri();
				}

				@Override
				public ServiceInstance choose(String serviceId) {
					List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
					int instanceIdx = random.nextInt(instances.size());
					return instances.get(instanceIdx);
				}
			};
		}

	}
}
