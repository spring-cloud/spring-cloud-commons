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
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 * @author Charu Covindane
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
		DefaultServiceInstance instance = new DefaultServiceInstance();
		instance.setServiceId("testservice");
		instance.setHost("localhost");
		instance.setPort(this.port);
		this.properties.getInstances().put("testservice", Arrays.asList(instance));
	}

	@Test
	public void testFilterFunctionWorks() {
		String value = WebClient.builder().baseUrl("http://testservice")
				.filter(this.lbFunction).build().get().uri("/hello").retrieve()
				.bodyToMono(String.class).block();
		then(value).isEqualTo("Hello World");
	}

	@Test
	public void testNoInstance() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://foobar")
				.filter(this.lbFunction).build().get().exchange().block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	public void testNoHostName() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http:///foobar")
				.filter(this.lbFunction).build().get().exchange().block();
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
		LoadBalancerClient loadBalancerClient(DiscoveryClient discoveryClient) {
			return new LoadBalancerClient() {
				Random random = new Random();

				@Override
				public <T> T execute(String serviceId, LoadBalancerRequest<T> request)
						throws IOException {
					throw new UnsupportedOperationException();
				}

				@Override
				public <T> T execute(String serviceId, ServiceInstance serviceInstance,
						LoadBalancerRequest<T> request) throws IOException {
					throw new UnsupportedOperationException();
				}

				@Override
				public URI reconstructURI(ServiceInstance instance, URI original) {
					return UriComponentsBuilder.fromUri(original).host(instance.getHost())
							.port(instance.getPort()).build().toUri();
				}

				@Override
				public ServiceInstance choose(String serviceId) {
					List<ServiceInstance> instances = discoveryClient
							.getInstances(serviceId);
					if (instances.size() == 0) {
						return null;
					}
					int instanceIdx = this.random.nextInt(instances.size());
					return instances.get(instanceIdx);
				}
			};
		}

	}

}
