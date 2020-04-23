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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.CUSTOM_DISCOVERY_CLIENT;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.CUSTOM_SERVICE_ID;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.DEFAULT_ORDER_DISCOVERY_CLIENT;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.FOURTH_DISCOVERY_CLIENT;

/**
 * Tests for the support of ordered {@link DiscoveryClient} instances in
 * {@link CompositeDiscoveryClient}
 *
 * @author Olga Maciaszek-Sharma
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.discovery.client.simple.order:2",
		classes = { CompositeDiscoveryClientTestsConfig.class })
public class CompositeDiscoveryClientOrderTest {

	@Autowired
	DiscoveryClient discoveryClient;

	@Test
	public void shouldGetOrderedDiscoveryClients() {
		// when:
		List<DiscoveryClient> discoveryClients = ((CompositeDiscoveryClient) this.discoveryClient)
				.getDiscoveryClients();

		// then:
		then(discoveryClients.get(0).description()).isEqualTo(CUSTOM_DISCOVERY_CLIENT);
		then(discoveryClients.get(1).description())
				.isEqualTo(DEFAULT_ORDER_DISCOVERY_CLIENT);
		then(discoveryClients.get(2).description()).isEqualTo("Simple Discovery Client");
		then(discoveryClients.get(3).description()).isEqualTo(FOURTH_DISCOVERY_CLIENT);
	}

	@Test
	public void shouldOnlyReturnServiceInstancesForTheHighestPrecedenceDiscoveryClient() {
		// when:
		List<ServiceInstance> serviceInstances = this.discoveryClient
				.getInstances(CUSTOM_SERVICE_ID);

		// then:
		then(serviceInstances).hasSize(1);
		then(serviceInstances.get(0).getPort()).isEqualTo(123);
	}

}
