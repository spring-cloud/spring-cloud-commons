/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer.reactive.endpoint;

import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties.SimpleServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "management.endpoints.web.exposure.include=loadbalancer" },
		webEnvironment = RANDOM_PORT)
@ActiveProfiles("lbendpoint")
public class LoadBalancerEndpointTests {

	private static final Random random = new Random();

	@Autowired
	private WebTestClient client;

	@Test
	public void chooseWorks() {
		client.get().uri("/actuator/loadbalancer/myservice/choose").exchange()
				.expectBody(SimpleServiceInstance.class).consumeWith(result -> {
					ServiceInstance body = result.getResponseBody();
					assertThat(body).isNotNull();
					assertThat(body.getHost()).isNotEmpty();
				});

	}

	@Test
	public void chooseWorksWithoutInstances() {
		client.get().uri("/actuator/loadbalancer/unknownservice/choose").exchange()
				.expectBody(SimpleServiceInstance.class).consumeWith(result -> {
					ServiceInstance body = result.getResponseBody();
					assertThat(body).isNull();
					// TODO: web response 404?
				});

	}

	static Mono<Response<ServiceInstance>> choose(ReactiveDiscoveryClient discoveryClient,
			String serviceId) {
		return discoveryClient.getInstances(serviceId).collectList().map(instances -> {
			if (instances.isEmpty()) {
				return new InstanceResponse(null);
			}
			int idx = random.nextInt(instances.size());
			ServiceInstance instance = instances.get(idx);
			return new InstanceResponse(instance);
		});
	}

	private static final class InstanceResponse implements Response<ServiceInstance> {

		private final ServiceInstance instance;

		private InstanceResponse(ServiceInstance instance) {
			this.instance = instance;
		}

		@Override
		public boolean hasServer() {
			return this.instance != null;
		}

		@Override
		public ServiceInstance getServer() {
			return this.instance;
		}

		@Override
		public void onComplete(CompletionContext completionContext) {

		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestConfiguration {

		@Bean
		public ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory(
				ReactiveDiscoveryClient discoveryClient) {
			return serviceId -> (ReactiveLoadBalancer<ServiceInstance>) request -> choose(
					discoveryClient, serviceId);
		};

	}

}
