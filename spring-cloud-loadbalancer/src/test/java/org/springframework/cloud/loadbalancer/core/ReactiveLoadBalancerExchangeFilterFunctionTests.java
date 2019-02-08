/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Nikola Kološnjaji
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

		@RequestMapping
		public String hello(@RequestParam("param") String param) {
			return "Hello " + param;
		}

	}

}
