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

package org.springframework.cloud.loadbalancer.support.grpc;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author KouShenhai（laokou）
 */
class DiscoveryNameResolverTest {

	private DiscoveryClient discoveryClient;

	private NameResolver.Listener2 listener;

	private DiscoveryNameResolver resolver;

	private final String serviceName = "test";

	@BeforeEach
	void setUp() {
		discoveryClient = Mockito.mock(DiscoveryClient.class);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		NameResolver.Args args = Mockito.mock(NameResolver.Args.class);
		listener = Mockito.mock(NameResolver.Listener2.class);

		Mockito.when(args.getServiceConfigParser()).thenReturn(Mockito.mock(NameResolver.ServiceConfigParser.class));

		resolver = new DiscoveryNameResolver(serviceName, discoveryClient, executorService);
	}

	@Test
	void testGetServiceAuthority() {
		assertThat(resolver.getServiceAuthority()).isEqualTo(serviceName);
	}

	@Test
	void testStartAndResolve() throws InterruptedException {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("grpc_port", "9091");
		ServiceInstance instance = new DefaultServiceInstance(serviceName + "-1", serviceName, "localhost", 9090, false,
				metadata);
		Mockito.when(discoveryClient.getInstances(serviceName)).thenReturn(List.of(instance));

		resolver.start(listener);

		// Wait for async execution
		Thread.sleep(200);

		ArgumentCaptor<NameResolver.ResolutionResult> resultCaptor = ArgumentCaptor
			.forClass(NameResolver.ResolutionResult.class);
		verify(listener).onResult(resultCaptor.capture());

		NameResolver.ResolutionResult result = resultCaptor.getValue();
		List<EquivalentAddressGroup> list = result.getAddressesOrError().getValue();
		assertThat(list).hasSize(1);
		assertThat(list.get(0).getAddresses().get(0).toString()).contains("9091");
	}

	@Test
	void testResolveNoInstances() throws InterruptedException {
		Mockito.when(discoveryClient.getInstances(serviceName)).thenReturn(Collections.emptyList());

		resolver.start(listener);

		// Wait for async execution
		Thread.sleep(200);

		verify(listener).onError(Mockito.any());
	}

	@Test
	void testRefresh() throws InterruptedException {
		Mockito.when(discoveryClient.getInstances(serviceName)).thenReturn(Collections.emptyList());
		resolver.start(listener);
		Thread.sleep(100);

		resolver.refresh();
		Thread.sleep(100);

		// verify called twice (start and refresh)
		verify(discoveryClient, Mockito.times(2)).getInstances(serviceName);
	}

	@Test
	void testRefreshFromExternal() throws InterruptedException {
		Mockito.when(discoveryClient.getInstances(serviceName)).thenReturn(Collections.emptyList());
		resolver.start(listener);
		Thread.sleep(100);

		resolver.refreshFromExternal();
		Thread.sleep(100);

		verify(discoveryClient, Mockito.times(2)).getInstances(serviceName);
	}

}
