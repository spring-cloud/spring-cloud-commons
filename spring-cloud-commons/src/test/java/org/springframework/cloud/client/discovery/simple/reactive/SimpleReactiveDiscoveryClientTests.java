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

package org.springframework.cloud.client.discovery.simple.reactive;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Ysewyn
 * @author Charu Covindane
 */
public class SimpleReactiveDiscoveryClientTests {

	private final DefaultServiceInstance service1Inst1 = new DefaultServiceInstance(null,
			null, "host1", 8080, false);

	private final DefaultServiceInstance service1Inst2 = new DefaultServiceInstance(null,
			null, "host2", 8443, true);

	private SimpleReactiveDiscoveryClient client;

	@BeforeEach
	public void setUp() {
		SimpleReactiveDiscoveryProperties simpleReactiveDiscoveryProperties = new SimpleReactiveDiscoveryProperties();
		simpleReactiveDiscoveryProperties.setInstances(
				singletonMap("service", Arrays.asList(service1Inst1, service1Inst2)));
		simpleReactiveDiscoveryProperties.init();
		this.client = new SimpleReactiveDiscoveryClient(
				simpleReactiveDiscoveryProperties);
	}

	@Test
	public void verifyDefaults() {
		assertThat(client.description()).isEqualTo("Simple Reactive Discovery Client");
		assertThat(client.getOrder()).isEqualTo(ReactiveDiscoveryClient.DEFAULT_ORDER);
	}

	@Test
	public void shouldReturnFluxOfServices() {
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNext("service").expectComplete().verify();
	}

	@Test
	public void shouldReturnEmptyFluxForNonExistingService() {
		Flux<ServiceInstance> instances = this.client.getInstances("undefined");
		StepVerifier.create(instances).expectComplete();
	}

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		Flux<ServiceInstance> services = this.client.getInstances("service");
		StepVerifier.create(services).expectNext(service1Inst1).expectNext(service1Inst2)
				.expectComplete().verify();
	}

}
