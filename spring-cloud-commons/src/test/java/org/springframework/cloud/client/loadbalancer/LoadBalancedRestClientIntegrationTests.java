/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for load-balanced {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(
		classes = { LoadBalancedRestClientIntegrationTests.TestConfig.class, LoadBalancerAutoConfiguration.class },
		properties = "spring.cloud.loadbalancer.retry.enabled=false")
public class LoadBalancedRestClientIntegrationTests {

	private final RestClient client;

	@Autowired
	ApplicationContext context;

	public LoadBalancedRestClientIntegrationTests(@Autowired RestClient.Builder clientBuilder) {
		this.client = clientBuilder.build();
	}

	@Test
	void shouldBuildLoadBalancedRestClientInConstructor() {
		// Interceptors are not visible in RestClient
		assertThatThrownBy(() -> client.get().uri("http://test-service").retrieve())
			.hasMessage("LoadBalancerInterceptor invoked.");
	}

	@SpringBootConfiguration
	static class TestConfig {

		@LoadBalanced
		@Bean
		RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}

		@Bean
		LoadBalancerClient testLoadBalancerClient() {
			return new LoadBalancerClient() {
				@Override
				public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
					throw new UnsupportedOperationException("LoadBalancerInterceptor invoked.");
				}

				@Override
				public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request)
						throws IOException {
					throw new UnsupportedOperationException("LoadBalancerInterceptor invoked.");
				}

				@Override
				public URI reconstructURI(ServiceInstance instance, URI original) {
					throw new UnsupportedOperationException("LoadBalancerInterceptor invoked.");
				}

				@Override
				public ServiceInstance choose(String serviceId) {
					throw new UnsupportedOperationException("LoadBalancerInterceptor invoked.");
				}

				@Override
				public <T> ServiceInstance choose(String serviceId, Request<T> request) {
					throw new UnsupportedOperationException("LoadBalancerInterceptor invoked.");
				}
			};
		}

	}

}
