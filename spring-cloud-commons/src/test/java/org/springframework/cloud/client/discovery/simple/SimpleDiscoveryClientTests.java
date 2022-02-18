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

package org.springframework.cloud.client.discovery.simple;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Biju Kunjummen
 * @author Charu Covindane
 * @author Neil Powell
 */
public class SimpleDiscoveryClientTests {

	private SimpleDiscoveryClient simpleDiscoveryClient;

	@BeforeEach
	public void setUp() {
		SimpleDiscoveryProperties simpleDiscoveryProperties = new SimpleDiscoveryProperties();

		Map<String, List<DefaultServiceInstance>> map = new HashMap<>();
		DefaultServiceInstance service1Inst1 = new DefaultServiceInstance(null, null, "host1", 8080, false);
		DefaultServiceInstance service1Inst2 = new DefaultServiceInstance(null, null, "host2", 0, true);
		DefaultServiceInstance service1Inst3 = new DefaultServiceInstance(null, null, "host3", 0, false);
		map.put("service1", Arrays.asList(service1Inst1, service1Inst2, service1Inst3));
		simpleDiscoveryProperties.setInstances(map);
		simpleDiscoveryProperties.init();
		this.simpleDiscoveryClient = new SimpleDiscoveryClient(simpleDiscoveryProperties);
	}

	@Test
	public void shouldBeAbleToRetrieveServiceDetailsByName() {
		List<ServiceInstance> instances = this.simpleDiscoveryClient.getInstances("service1");
		then(instances.size()).isEqualTo(3);
		then(instances.get(0).getServiceId()).isEqualTo("service1");
		then(instances.get(0).getHost()).isEqualTo("host1");
		then(instances.get(0).getPort()).isEqualTo(8080);
		then(instances.get(0).getUri()).isEqualTo(URI.create("http://host1:8080"));
		then(instances.get(0).isSecure()).isEqualTo(false);
		then(instances.get(0).getMetadata()).isNotNull();

		then(instances.get(1).getServiceId()).isEqualTo("service1");
		then(instances.get(1).getHost()).isEqualTo("host2");
		then(instances.get(1).getPort()).isEqualTo(0);
		then(instances.get(1).getUri()).isEqualTo(URI.create("https://host2:443"));
		then(instances.get(1).isSecure()).isEqualTo(true);
		then(instances.get(1).getMetadata()).isNotNull();

		then(instances.get(2).getServiceId()).isEqualTo("service1");
		then(instances.get(2).getHost()).isEqualTo("host3");
		then(instances.get(2).getPort()).isEqualTo(0);
		then(instances.get(2).getUri()).isEqualTo(URI.create("http://host3:80"));
		then(instances.get(2).isSecure()).isEqualTo(false);
		then(instances.get(2).getMetadata()).isNotNull();
	}

}
