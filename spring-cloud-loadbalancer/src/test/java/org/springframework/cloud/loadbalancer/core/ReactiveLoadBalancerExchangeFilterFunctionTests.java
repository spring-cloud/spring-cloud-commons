package org.springframework.cloud.loadbalancer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.URI;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties.SimpleServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.LoadBalancerTest.MyServiceConfig;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ReactiveLoadBalancerExchangeFilterFunctionTests {

	@Autowired
	private ReactiveLoadBalancerExchangeFilterFunction lbFunction;

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
		String value = WebClient.builder().baseUrl("http://testservice")
				.filter(lbFunction).build().get().uri("/hello?param=World").retrieve()
				.bodyToMono(String.class).block();
		assertThat(value).isEqualTo("Hello World");
	}

	@Test
	public void testNoInstance() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://foobar")
				.filter(lbFunction).build().get().exchange().block();
		assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	public void testNoHostName() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http:///foobar")
				.filter(lbFunction).build().get().exchange().block();
		assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@EnableDiscoveryClient
	@LoadBalancerClients({
			@LoadBalancerClient(name = "testservice", configuration = MyServiceConfig.class) })
	@EnableAutoConfiguration
	@SpringBootConfiguration
	@RestController
	static class Config {

		@RequestMapping()
		public String hello(@RequestParam("param") String param) {
			return "Hello " + param;
		}
	}
}
