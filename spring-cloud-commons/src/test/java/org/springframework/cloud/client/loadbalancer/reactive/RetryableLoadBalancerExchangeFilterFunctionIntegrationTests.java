/*
 * Copyright 2012-2021 the original author or authors.
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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for {@link RetryableLoadBalancerExchangeFilterFunction}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RetryableLoadBalancerExchangeFilterFunctionIntegrationTests {

	@Autowired
	private RetryableLoadBalancerExchangeFilterFunction loadBalancerFunction;

	@Autowired
	private SimpleDiscoveryProperties properties;

	@Autowired
	private LoadBalancerRetryProperties retryProperties;

	@LocalServerPort
	private int port;

	@BeforeEach
	void setUp() {
		DefaultServiceInstance instance = new DefaultServiceInstance();
		instance.setServiceId("testservice");
		instance.setUri(URI.create("http://localhost:" + port));
		DefaultServiceInstance instanceWithNoLifecycleProcessors = new DefaultServiceInstance();
		instanceWithNoLifecycleProcessors
				.setServiceId("serviceWithNoLifecycleProcessors");
		instanceWithNoLifecycleProcessors.setUri(URI.create("http://localhost:" + port));
		properties.getInstances().put("testservice", Collections.singletonList(instance));
		properties.getInstances().put("serviceWithNoLifecycleProcessors",
				Collections.singletonList(instanceWithNoLifecycleProcessors));
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
	void correctResponseReturnedAfterRetryingOnSameServiceInstance() {
		retryProperties.setMaxRetriesOnSameServiceInstance(1);
		retryProperties.getRetryableStatusCodes().add(500);

		ClientResponse clientResponse = WebClient.builder().baseUrl("http://testservice")
				.filter(this.loadBalancerFunction).build().get().uri("/exception")
				.exchange().block();

		then(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
		then(clientResponse.bodyToMono(String.class).block()).isEqualTo("Hello World!");
	}

	// FIXME - flaky test
	@Disabled
	@Test
	void correctResponseReturnedAfterRetryingOnNextServiceInstanceWithBackoff() {
		retryProperties.getBackoff().setEnabled(true);
		retryProperties.setMaxRetriesOnSameServiceInstance(1);
		DefaultServiceInstance goodRetryTestInstance = new DefaultServiceInstance();
		goodRetryTestInstance.setServiceId("retrytest");
		goodRetryTestInstance.setUri(URI.create("http://localhost:" + port));
		DefaultServiceInstance badRetryTestInstance = new DefaultServiceInstance();
		badRetryTestInstance.setServiceId("retrytest");
		badRetryTestInstance.setUri(URI.create("http://localhost:" + 8080));
		properties.getInstances().put("retrytest",
				Arrays.asList(badRetryTestInstance, goodRetryTestInstance));
		retryProperties.getRetryableStatusCodes().add(500);

		ClientResponse clientResponse = WebClient.builder().baseUrl("http://retrytest")
				.filter(this.loadBalancerFunction).build().get().uri("/hello").exchange()
				.block();

		then(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
		then(clientResponse.bodyToMono(String.class).block()).isEqualTo("Hello World");

		ClientResponse secondClientResponse = WebClient.builder()
				.baseUrl("http://retrytest").filter(this.loadBalancerFunction).build()
				.get().uri("/hello").exchange().block();

		then(secondClientResponse.statusCode()).isEqualTo(HttpStatus.OK);
		then(secondClientResponse.bodyToMono(String.class).block())
				.isEqualTo("Hello World");
	}

	@Test
	void serviceUnavailableReturnedWhenNoInstancePresent() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://xxx")
				.filter(this.loadBalancerFunction).build().get().exchange().block();

		then(clientResponse.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	@Disabled
	// FIXME 3.0.0
	void badRequestReturnedForIncorrectHost() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http:///xxx")
				.filter(this.loadBalancerFunction).build().get().exchange().block();

		then(clientResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void exceptionNotThrownWhenFactoryReturnsNullLifecycleProcessorsMap() {
		assertThatCode(() -> WebClient.builder()
				.baseUrl("http://serviceWithNoLifecycleProcessors")
				.filter(this.loadBalancerFunction).build().get().uri("/hello").exchange()
				.block()).doesNotThrowAnyException();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@EnableDiscoveryClient
	@EnableAutoConfiguration
	@SpringBootConfiguration(proxyBeanMethods = false)
	@RestController
	static class Config {

		AtomicInteger exceptionCallsCount = new AtomicInteger();

		@GetMapping("/hello")
		public String hello() {
			return "Hello World";
		}

		@GetMapping("/callback")
		String callbackTestResult() {
			return "callbackTestResult";
		}

		@GetMapping("/exception")
		String exception() {
			int callCount = exceptionCallsCount.incrementAndGet();
			if (callCount % 2 != 0) {
				throw new IllegalStateException("Test!");
			}
			return "Hello World!";
		}

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory(
				DiscoveryClient discoveryClient) {
			return serviceId -> new DiscoveryClientBasedReactiveLoadBalancer(serviceId,
					discoveryClient);
		}

		@Bean
		LoadBalancerRetryProperties loadBalancerRetryProperties() {
			return new LoadBalancerRetryProperties();
		}

		@Bean
		RetryableLoadBalancerExchangeFilterFunction exchangeFilterFunction(
				LoadBalancerRetryProperties properties,
				ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
			return new RetryableLoadBalancerExchangeFilterFunction(
					new RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(
							properties),
					factory, properties, Collections.emptyList());
		}

	}

}
