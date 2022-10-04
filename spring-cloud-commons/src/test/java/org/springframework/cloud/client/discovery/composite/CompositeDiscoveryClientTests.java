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

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientTestsConfig.CUSTOM_SERVICE_ID;

/**
 * Tests for behavior of Composite Discovery Client.
 *
 * @author Biju Kunjummen
 */

@SpringBootTest(
		properties = { "spring.application.name=service0",
				"spring.cloud.discovery.client.simple.instances.service1[0].uri=http://s11:8080",
				"spring.cloud.discovery.client.simple.instances.service1[1].uri=https://s12:8443",
				"spring.cloud.discovery.client.simple.instances.service2[0].uri=https://s21:8080",
				"spring.cloud.discovery.client.simple.instances.service2[1].uri=https://s22:443" },
		classes = { CompositeDiscoveryClientTestsConfig.class })
public class CompositeDiscoveryClientTests {

	@Autowired
	private DiscoveryClient discoveryClient;

	@Test
	public void getInstancesByServiceIdShouldDelegateCall() {
		then(this.discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);

		then(this.discoveryClient.getInstances("service1")).hasSize(2);

		ServiceInstance s1 = this.discoveryClient.getInstances("service1").get(0);
		then(s1.getHost()).isEqualTo("s11");
		then(s1.getPort()).isEqualTo(8080);
		then(s1.getUri()).isEqualTo(URI.create("http://s11:8080"));
		then(s1.isSecure()).isEqualTo(false);
	}

	@Test
	public void getServicesShouldAggregateAllServiceNames() {
		then(this.discoveryClient.getServices()).containsOnlyOnce("service1", "service2", "custom");
	}

	@Test
	public void getDescriptionShouldBeComposite() {
		then(this.discoveryClient.description()).isEqualTo("Composite Discovery Client");
	}

	@Test
	public void getInstancesShouldRespectOrder() {
		then(this.discoveryClient.getInstances(CUSTOM_SERVICE_ID)).hasSize(1);
		then(this.discoveryClient.getInstances(CUSTOM_SERVICE_ID)).hasSize(1);
	}

	@Test
	public void getInstancesByUnknownServiceIdShouldReturnAnEmptyList() {
		then(this.discoveryClient.getInstances("unknown")).hasSize(0);
	}

}
