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

package org.springframework.cloud.client.discovery.composite;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;

/**
 * Test configuration for {@link CompositeDiscoveryClient} tests.
 *
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
public class CompositeDiscoveryClientTestsConfig {

	static final String DEFAULT_ORDER_DISCOVERY_CLIENT = "Default order discovery client";
	static final String CUSTOM_DISCOVERY_CLIENT = "A custom discovery client";
	static final String FOURTH_DISCOVERY_CLIENT = "Fourth discovery client";
	static final String CUSTOM_SERVICE_ID = "custom";

	@Bean
	public DiscoveryClient customDiscoveryClient() {
		return aDiscoveryClient(-1, CUSTOM_DISCOVERY_CLIENT);
	}

	@Bean
	public DiscoveryClient thirdOrderCustomDiscoveryClient() {
		return aDiscoveryClient(3, FOURTH_DISCOVERY_CLIENT);
	}

	@Bean
	public DiscoveryClient defaultOrderDiscoveryClient() {
		return aDiscoveryClient(null, DEFAULT_ORDER_DISCOVERY_CLIENT);
	}

	private DiscoveryClient aDiscoveryClient(Integer order, String description) {
		return new DiscoveryClient() {
			@Override
			public String description() {
				return description;
			}

			@Override
			public List<ServiceInstance> getInstances(String serviceId) {
				if (serviceId.equals(CUSTOM_SERVICE_ID)) {
					ServiceInstance s1 = new DefaultServiceInstance("customInstance", CUSTOM_SERVICE_ID, "host", 123,
							false);
					return singletonList(s1);
				}
				return Collections.emptyList();
			}

			@Override
			public List<String> getServices() {
				return singletonList(CUSTOM_SERVICE_ID);
			}

			@Override
			public int getOrder() {
				return order != null ? order : DiscoveryClient.super.getOrder();
			}
		};
	}

}
