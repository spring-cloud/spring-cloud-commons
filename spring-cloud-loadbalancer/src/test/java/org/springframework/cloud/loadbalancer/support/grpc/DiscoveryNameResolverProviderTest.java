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

import io.grpc.NameResolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

/**
 * @author KouShenhai（laokou）
 */
class DiscoveryNameResolverProviderTest {

	private DiscoveryNameResolverProvider provider;

	@BeforeEach
	void setUp() {
		DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		provider = new DiscoveryNameResolverProvider(discoveryClient, executorService);
	}

	@Test
	void testIsAvailable() {
		Assertions.assertThat(provider.isAvailable()).isTrue();
	}

	@Test
	void testPriority() {
		Assertions.assertThat(provider.priority()).isEqualTo(6);
	}

	@Test
	void testGetDefaultScheme() {
		Assertions.assertThat(provider.getDefaultScheme()).isEqualTo("discovery");
	}

	@Test
	void testNewNameResolver() {
		URI uri = URI.create("discovery://test-service");
		NameResolver.Args args = mock(NameResolver.Args.class);
		Mockito.when(args.getServiceConfigParser()).thenReturn(mock(NameResolver.ServiceConfigParser.class));

		NameResolver resolver = provider.newNameResolver(uri, args);
		Assertions.assertThat(resolver).isExactlyInstanceOf(DiscoveryNameResolver.class);
		Assertions.assertThat(resolver.getServiceAuthority()).isEqualTo("test-service");
	}

	@Test
	void testOnHeartbeatEvent() {
		URI uri = URI.create("discovery://test-service");
		NameResolver.Args args = mock(NameResolver.Args.class);
		Mockito.when(args.getServiceConfigParser()).thenReturn(mock(NameResolver.ServiceConfigParser.class));

		// Register a resolver
		provider.newNameResolver(uri, args);

		HeartbeatEvent event = new HeartbeatEvent(new Object(), "test");
		provider.onHeartbeatEvent(event);
	}

}
