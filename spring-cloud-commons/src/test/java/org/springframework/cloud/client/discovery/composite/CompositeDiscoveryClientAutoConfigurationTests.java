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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Composite Discovery Client should be the one found by default.
 *
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class CompositeDiscoveryClientAutoConfigurationTests {

	@Autowired
	private DiscoveryClient discoveryClient;

	@Test
	public void compositeDiscoveryClientShouldBeTheDefault() {
		then(this.discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);
		CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) this.discoveryClient;
		then(compositeDiscoveryClient.getDiscoveryClients()).hasSize(2);
		then(compositeDiscoveryClient.getDiscoveryClients().get(0).description())
				.isEqualTo("A custom discovery client");
	}

	@Test
	public void simpleDiscoveryClientShouldBeHaveTheLowestPrecedence() {
		CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) this.discoveryClient;
		then(compositeDiscoveryClient.getDiscoveryClients().get(0).description())
				.isEqualTo("A custom discovery client");
		then(compositeDiscoveryClient.getDiscoveryClients().get(1))
				.isInstanceOf(SimpleDiscoveryClient.class);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		public DiscoveryClient customDiscoveryClient1() {
			return new DiscoveryClient() {
				@Override
				public String description() {
					return "A custom discovery client";
				}

				@Override
				public List<ServiceInstance> getInstances(String serviceId) {
					return null;
				}

				@Override
				public List<String> getServices() {
					return null;
				}
			};
		}

	}

}
