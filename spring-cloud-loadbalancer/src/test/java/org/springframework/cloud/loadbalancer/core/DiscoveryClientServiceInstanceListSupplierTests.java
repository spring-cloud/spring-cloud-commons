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

package org.springframework.cloud.loadbalancer.core;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DiscoveryClientServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
class DiscoveryClientServiceInstanceListSupplierTests {

	private static final String SERVICE_ID = "test";

	private final MockEnvironment environment = new MockEnvironment();

	private final ReactiveDiscoveryClient reactiveDiscoveryClient = mock(
			ReactiveDiscoveryClient.class);

	private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

	private DiscoveryClientServiceInstanceListSupplier supplier;

	private static DefaultServiceInstance instance(String host, boolean secure) {
		return new DefaultServiceInstance(SERVICE_ID, SERVICE_ID, host, 80, secure);
	}

	@BeforeEach
	void setUp() {
		environment.setProperty("loadbalancer.client.name", SERVICE_ID);
	}

	@Test
	void shouldReturnRetrievedInstances() {
		when(reactiveDiscoveryClient.getInstances(SERVICE_ID)).thenReturn(
				Flux.just(instance("1host", false), instance("2host-secure", true)));

		StepVerifier.withVirtualTime(() -> {
			supplier = new DiscoveryClientServiceInstanceListSupplier(
					reactiveDiscoveryClient, environment);
			return supplier.get();
		}).expectSubscription().expectNext(
				Lists.list(instance("1host", false), instance("2host-secure", true)))
				.thenCancel().verify();
	}

	@Test
	void shouldUpdateReturnRetrievedInstances() {
		when(reactiveDiscoveryClient.getInstances(SERVICE_ID)).thenReturn(
				Flux.just(instance("1host", false), instance("2host-secure", true)));
		supplier = new DiscoveryClientServiceInstanceListSupplier(reactiveDiscoveryClient,
				environment);

		StepVerifier.withVirtualTime(() -> supplier.get()).expectSubscription()
				.expectNext(Lists.list(instance("1host", false),
						instance("2host-secure", true)))
				.thenCancel().verify();

		when(reactiveDiscoveryClient.getInstances(SERVICE_ID))
				.thenReturn(Flux.just(instance("1host", false),
						instance("2host-secure", true), instance("3host", false)));

		StepVerifier.withVirtualTime(() -> supplier.get()).expectSubscription()
				.expectNext(Lists.list(instance("1host", false),
						instance("2host-secure", true), instance("3host", false)))
				.thenCancel().verify();
	}

	@Test
	void shouldReturnRetrievedInstancesBlockingClient() {
		StepVerifier.withVirtualTime(() -> {
			when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(
					Lists.list(instance("1host", false), instance("2host-secure", true)));

			supplier = new DiscoveryClientServiceInstanceListSupplier(discoveryClient,
					environment);
			return supplier.get();
		}).expectSubscription().expectNext(
				Lists.list(instance("1host", false), instance("2host-secure", true)))
				.thenCancel().verify();
	}

	@Test
	void shouldUpdateReturnRetrievedInstancesBlockingClient() {
		StepVerifier.withVirtualTime(() -> {
			when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(
					Lists.list(instance("1host", false), instance("2host-secure", true)));
			supplier = new DiscoveryClientServiceInstanceListSupplier(discoveryClient,
					environment);
			supplier.get();

			when(discoveryClient.getInstances(SERVICE_ID))
					.thenReturn(Lists.list(instance("1host", false),
							instance("2host-secure", true), instance("3host", false)));
			return supplier.get();
		}).expectSubscription()
				.expectNext(Lists.list(instance("1host", false),
						instance("2host-secure", true), instance("3host", false)))
				.thenCancel().verify();
	}

}
