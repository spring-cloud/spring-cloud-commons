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

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author KouShenhai（laokou）
 */
class DiscoveryGrpcChannelFactoryTest {

	private DiscoveryGrpcChannelFactory factory;

	@BeforeEach
	void setUp() {
		ClientInterceptorsConfigurer configurer = mock(ClientInterceptorsConfigurer.class);
		factory = new DiscoveryGrpcChannelFactory(Collections.emptyList(), configurer);
	}

	@Test
	void testSupports() {
		assertThat(factory.supports("discovery:test-service")).isTrue();
		assertThat(factory.supports("static:localhost")).isFalse();
		assertThat(factory.supports("dns:localhost")).isFalse();
	}

	@Test
	void testNewChannelBuilder() {
		String target = "test-service";
		ChannelCredentials credentials = InsecureChannelCredentials.create();
		NettyChannelBuilder builder = factory.newChannelBuilder(target, credentials);
		assertThat(builder).isNotNull();
	}

}
