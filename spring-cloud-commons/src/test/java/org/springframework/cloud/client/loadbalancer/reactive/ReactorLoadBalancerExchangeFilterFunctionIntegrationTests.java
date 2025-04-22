/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for {@link ReactorLoadBalancerExchangeFilterFunction}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 */
@SuppressWarnings("ConstantConditions")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ReactorLoadBalancerExchangeFilterFunctionIntegrationTests
		extends AbstractLoadBalancerExchangeFilterFunctionIntegrationTests {

	@EnableDiscoveryClient
	@EnableAutoConfiguration
	@SpringBootConfiguration(proxyBeanMethods = false)
	@RestController
	static class Config {

		@GetMapping("/hello")
		public String hello() {
			return "Hello World";
		}

		@GetMapping("/callback")
		String callbackTestResult() {
			return "callbackTestResult";
		}

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory(DiscoveryClient discoveryClient,
				LoadBalancerProperties properties) {
			return new TestLoadBalancerFactory(discoveryClient, properties);
		}

	}

}
