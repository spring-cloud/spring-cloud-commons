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

package org.springframework.cloud.loadbalancer.blocking.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class BlockingLoadBalancerClientTests {

	@Autowired
	private BlockingLoadBalancerClient loadBalancerClient;

	@Autowired
	private SimpleDiscoveryProperties properties;

	@BeforeEach
	void setUp() {
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(null, null,
				"test.example", 9999, true);
		properties.getInstances().put("myservice",
				Collections.singletonList(serviceInstance));
	}

	@Test
	void correctServiceInstanceChosen() {
		ServiceInstance serviceInstance = loadBalancerClient.choose("myservice");
		assertThat(serviceInstance.getHost()).isEqualTo("test.example");
	}

	@Test
	void nullReturnedIfInstanceMissing() {
		ServiceInstance serviceInstance = loadBalancerClient.choose("unknownservice");
		assertThat(serviceInstance).isNull();
	}

	@Test
	void requestExecutedAgainstCorrectInstance() throws IOException {
		final String result = "result";
		Object actualResult = loadBalancerClient.execute("myservice",
				(LoadBalancerRequest<Object>) instance -> {
					assertThat(instance.getHost()).isEqualTo("test.example");
					return result;
				});
		assertThat(actualResult).isEqualTo(result);
	}

	@Test
	void exceptionThrownIfInstanceNotAvailableForRequestExecution() {
		try {
			final String result = "result";
			Object actualResult = loadBalancerClient.execute("unknownservice",
					(LoadBalancerRequest<Object>) instance -> result);
			assertThat(actualResult).isEqualTo(result);
			fail("Should have thrown exception.");
		}
		catch (Exception exception) {
			assertThat(exception).isNotNull();
			assertThat(exception).isInstanceOf(IllegalStateException.class);
			assertThat(exception).hasMessage("No instances available for unknownservice");
		}
	}

	@Test
	void exceptionRethrownAsRuntime() {
		try {
			loadBalancerClient.execute("myservice", instance -> {
				assertThat(instance.getHost()).isEqualTo("test.example");
				throw new Exception("Should throw exception.");
			});
			fail("Should have thrown exception.");
		}
		catch (Exception exception) {
			assertThat(exception).isNotNull();
			assertThat(exception).isInstanceOf(RuntimeException.class);
		}
	}

	@Test
	void IOExceptionRethrown() {
		try {
			loadBalancerClient.execute("myservice", instance -> {
				assertThat(instance.getHost()).isEqualTo("test.example");
				throw new IOException("Should throw IO exception.");
			});
			fail("Should have thrown exception.");
		}
		catch (Exception exception) {
			assertThat(exception).isNotNull();
			assertThat(exception).isInstanceOf(IOException.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@SpringBootConfiguration
	@LoadBalancerClients({
			@org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient(
					name = "myservice", configuration = MyServiceConfig.class),
			@org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient(
					name = "unknownservice",
					configuration = UnknownServiceConfig.class) })
	protected static class Config {

	}

	protected static class MyServiceConfig {

		@Bean
		ReactorLoadBalancer<ServiceInstance> reactiveLoadBalancer(
				DiscoveryClient discoveryClient) {
			return new DiscoveryClientBasedReactiveLoadBalancer("myservice",
					discoveryClient);
		}

	}

	protected static class UnknownServiceConfig {

		@Bean
		ReactorLoadBalancer<ServiceInstance> reactiveLoadBalancer(
				DiscoveryClient discoveryClient) {
			return new DiscoveryClientBasedReactiveLoadBalancer("unknownservice",
					discoveryClient);
		}

	}

}

class DiscoveryClientBasedReactiveLoadBalancer
		implements ReactorServiceInstanceLoadBalancer {

	private final Random random = new Random();

	private final String serviceId;

	private final DiscoveryClient discoveryClient;

	DiscoveryClientBasedReactiveLoadBalancer(String serviceId,
			DiscoveryClient discoveryClient) {
		this.serviceId = serviceId;
		this.discoveryClient = discoveryClient;
	}

	@Override
	public Mono<Response<ServiceInstance>> choose() {
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		if (instances.size() == 0) {
			return Mono.just(new EmptyResponse());
		}
		int instanceIdx = this.random.nextInt(instances.size());
		return Mono.just(new DefaultResponse(instances.get(instanceIdx)));
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		return choose();
	}

}
