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

package org.springframework.cloud.client.loadbalancer;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for load-balanced {@link RestTemplateBuilder}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = { LoadBalancedRestTemplateBuilderIntegrationTests.TestConfig.class,
		LoadBalancerAutoConfiguration.class }, properties = "spring.cloud.loadbalancer.retry.enabled=false")
public class LoadBalancedRestTemplateBuilderIntegrationTests {

	private final RestTemplateBuilder restTemplateBuilder;

	public LoadBalancedRestTemplateBuilderIntegrationTests(@Autowired RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	@Test
	void shouldBuildLoadBalancedRestTemplate() {
		RestTemplate restTemplate = restTemplateBuilder.build();

		assertThat(restTemplate.getInterceptors()).hasSize(1);
		assertThat(restTemplate.getInterceptors().get(0)).isInstanceOf(DeferringLoadBalancerInterceptor.class);
		assertThat(((DeferringLoadBalancerInterceptor) restTemplate.getInterceptors().get(0))
			.getLoadBalancerInterceptorProvider()
			.getObject()).isInstanceOf(BlockingLoadBalancerInterceptor.class);
	}

	@SpringBootConfiguration
	static class TestConfig {

		@LoadBalanced
		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder();
		}

		@Bean
		LoadBalancerClient testLoadBalancerClient() {
			return new LoadBalancerClient() {
				@Override
				public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
					throw new UnsupportedOperationException("LoadBalancerInterceptor invoked.");
				}

				@Override
				public <T> T execute(String serviceId, ServiceInstance serviceInstance,
						LoadBalancerRequest<T> request) {
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
