/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for {@link ReactorLoadBalancerExchangeFilterFunction}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ReactorLoadBalancerExchangeFilterFunctionTests {

	@Autowired
	private ReactorLoadBalancerExchangeFilterFunction loadBalancerFunction;

	@Autowired
	private SimpleDiscoveryProperties properties;

	@LocalServerPort
	private int port;

	@BeforeEach
	void setUp() {
		DefaultServiceInstance instance = new DefaultServiceInstance();
		instance.setServiceId("testservice");
		instance.setHost("localhost");
		instance.setPort(this.port);
		this.properties.getInstances().put("testservice",
				Collections.singletonList(instance));
	}

	@Test
	void correctResponseReturnedForExistingHostAndInstancePresent() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://testservice")
				.filter(this.loadBalancerFunction).build().get().uri("/hello").exchange()
				.block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
		then(clientResponse.bodyToMono(String.class).block()).isEqualTo("Hello World");
	}

	@Test
	void serviceUnavailableReturnedWhenNoInstancePresent() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://xxx")
				.filter(this.loadBalancerFunction).build().get().exchange().block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void badRequestReturnedForIncorrectHost() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http:///xxx")
				.filter(this.loadBalancerFunction).build().get().exchange().block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory(
				DiscoveryClient discoveryClient) {
			return serviceId -> new DiscoveryClientBasedReactiveLoadBalancer(serviceId,
					discoveryClient);
		}

	}

}

class DiscoveryClientBasedReactiveLoadBalancer
		implements ReactiveLoadBalancer<ServiceInstance> {

	private final Random random = new Random();

	private final String serviceId;

	private final DiscoveryClient discoveryClient;

	DiscoveryClientBasedReactiveLoadBalancer(String serviceId,
			DiscoveryClient discoveryClient) {
		this.serviceId = serviceId;
		this.discoveryClient = discoveryClient;
	}

	@Override
	public Publisher<Response<ServiceInstance>> choose() {
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		if (instances.size() == 0) {
			return Mono.just(new EmptyResponse());
		}
		int instanceIdx = this.random.nextInt(instances.size());
		return Mono.just(new DefaultResponse(instances.get(instanceIdx)));
	}

	@Override
	public Publisher<Response<ServiceInstance>> choose(Request request) {
		return choose();
	}

}
