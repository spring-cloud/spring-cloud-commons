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

package org.springframework.cloud.client.discovery.composite.reactive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
@ExtendWith(MockitoExtension.class)
class ReactiveCompositeDiscoveryClientTests {

	@Mock
	private ReactiveDiscoveryClient discoveryClient1;

	@Mock
	private ReactiveDiscoveryClient discoveryClient2;

	@Test
	public void shouldReturnEmptyFluxOfServices() {
		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(emptyList());

		Flux<String> services = client.getServices();

		StepVerifier.create(services).expectComplete().verify();
	}

	@Test
	public void shouldReturnFluxOfServices() {
		TestPublisher<String> discoveryClient1Publisher = TestPublisher.createCold();
		discoveryClient1Publisher.emit("serviceAFromClient1");
		discoveryClient1Publisher.emit("serviceBFromClient1");
		discoveryClient1Publisher.complete();

		TestPublisher<String> discoveryClient2Publisher = TestPublisher.createCold();
		discoveryClient2Publisher.emit("serviceCFromClient2");
		discoveryClient2Publisher.complete();

		when(discoveryClient1.getServices()).thenReturn(discoveryClient1Publisher.flux());
		when(discoveryClient2.getServices()).thenReturn(discoveryClient2Publisher.flux());

		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(
				asList(discoveryClient1, discoveryClient2));

		assertThat(client.description()).isEqualTo("Composite Reactive Discovery Client");

		Flux<String> services = client.getServices();

		StepVerifier.create(services).expectNext("serviceAFromClient1").expectNext("serviceBFromClient1")
				.expectNext("serviceCFromClient2").expectComplete().verify();
	}

	@Test
	public void shouldReturnEmptyFluxOfServiceInstances() {
		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(emptyList());

		Flux<ServiceInstance> instances = client.getInstances("service");

		StepVerifier.create(instances).expectComplete().verify();
	}

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		DefaultServiceInstance serviceInstance1 = new DefaultServiceInstance("instance", "service", "localhost", 8080,
				false);
		DefaultServiceInstance serviceInstance2 = new DefaultServiceInstance("instance2", "service", "localhost", 8080,
				false);
		TestPublisher<ServiceInstance> discoveryClient1Publisher = TestPublisher.createCold();
		discoveryClient1Publisher.emit(serviceInstance1);
		discoveryClient1Publisher.emit(serviceInstance2);
		discoveryClient1Publisher.complete();

		TestPublisher<ServiceInstance> discoveryClient2Publisher = TestPublisher.createCold();
		discoveryClient2Publisher.complete();

		when(discoveryClient1.getInstances("service")).thenReturn(discoveryClient1Publisher.flux());
		when(discoveryClient2.getInstances("service")).thenReturn(discoveryClient2Publisher.flux());

		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(
				asList(discoveryClient1, discoveryClient2));

		Flux<ServiceInstance> instances = client.getInstances("service");

		StepVerifier.create(instances).expectNext(serviceInstance1).expectNext(serviceInstance2).expectComplete()
				.verify();
	}

}
