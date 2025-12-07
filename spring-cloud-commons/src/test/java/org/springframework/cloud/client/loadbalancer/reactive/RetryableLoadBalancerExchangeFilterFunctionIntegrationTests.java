/*
 * Copyright 2012-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.InstanceProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for {@link RetryableLoadBalancerExchangeFilterFunction}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
@SuppressWarnings("DataFlowIssue")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RetryableLoadBalancerExchangeFilterFunctionIntegrationTests
		extends AbstractLoadBalancerExchangeFilterFunctionIntegrationTests {

	@Test
	void correctResponseReturnedAfterRetryingOnSameServiceInstance() {
		loadBalancerProperties.getRetry().setMaxRetriesOnSameServiceInstance(1);
		loadBalancerProperties.getRetry().getRetryableStatusCodes().add(500);

		ResponseEntity<String> response = WebClient.builder()
			.baseUrl("http://testservice")
			.filter(loadBalancerFunction)
			.build()
			.get()
			.uri("/exception")
			.retrieve()
			.toEntity(String.class)
			.block();

		then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(response.getBody()).isEqualTo("Hello World!");
	}

	@Test
	void correctResponseReturnedAfterRetryingOnNextServiceInstanceWithBackoff() {
		loadBalancerProperties.getRetry().getBackoff().setEnabled(true);
		loadBalancerProperties.getRetry().setMaxRetriesOnSameServiceInstance(1);
		InstanceProperties goodRetryTestInstance = new InstanceProperties();
		goodRetryTestInstance.setServiceId("retrytest");
		goodRetryTestInstance.setUri(URI.create("http://localhost:" + port));
		InstanceProperties badRetryTestInstance = new InstanceProperties();
		badRetryTestInstance.setServiceId("retrytest");
		badRetryTestInstance.setUri(URI.create("http://localhost:" + 8080));
		properties.getInstances().put("retrytest", Arrays.asList(badRetryTestInstance, goodRetryTestInstance));
		loadBalancerProperties.getRetry().getRetryableStatusCodes().add(500);

		ResponseEntity<String> response = WebClient.builder()
			.baseUrl("http://retrytest")
			.filter(loadBalancerFunction)
			.build()
			.get()
			.uri("/hello")
			.retrieve()
			.toEntity(String.class)
			.block();

		then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(response.getBody()).isEqualTo("Hello World");

		ResponseEntity<String> secondResponse = WebClient.builder()
			.baseUrl("http://retrytest")
			.filter(loadBalancerFunction)
			.build()
			.get()
			.uri("/hello")
			.retrieve()
			.toEntity(String.class)
			.block();

		then(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(secondResponse.getBody()).isEqualTo("Hello World");
	}

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
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory(DiscoveryClient discoveryClient,
				LoadBalancerProperties properties) {
			return new TestLoadBalancerFactory(discoveryClient, properties);
		}

		@Bean
		@Primary
		RetryableLoadBalancerExchangeFilterFunction exchangeFilterFunction(
				ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
			return new RetryableLoadBalancerExchangeFilterFunction(
					new RetryableExchangeFilterFunctionLoadBalancerRetryPolicy.Factory(factory), factory,
					Collections.emptyList());
		}

	}

}
