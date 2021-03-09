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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.net.URI;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LoadBalancerClientRequestTransformer}.
 *
 * @author Toshiaki Maki
 */
class LoadBalancerClientRequestTransformerTest {

	private final LoadBalancerRetryProperties properties = new LoadBalancerRetryProperties();

	private final LoadBalancerRetryPolicy policy = new RetryableExchangeFilterFunctionLoadBalancerRetryPolicy(
			properties);

	private final ReactiveLoadBalancer.Factory<ServiceInstance> factory = mock(
			ReactiveLoadBalancer.Factory.class);

	private final ClientRequest clientRequest = mock(ClientRequest.class);

	private final ClientResponse clientResponse = mock(ClientResponse.class);

	private final ExchangeFunction next = mock(ExchangeFunction.class);

	@BeforeEach
	void setUp() {
		when(factory.getInstance("testServiceId"))
				.thenReturn(new TestReactiveLoadBalancer());
		when(clientRequest.method()).thenReturn(HttpMethod.GET);
		when(clientRequest.url()).thenReturn(URI.create("http://testServiceId"));
		when(clientRequest.headers()).thenReturn(new HttpHeaders());
		when(clientRequest.cookies()).thenReturn(new LinkedMultiValueMap<>());
		when(next.exchange(any())).thenReturn(Mono.just(clientResponse));
		when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
	}

	@Test
	void transformReactorLoadBalancerExchangeFilterFunction() {
		ArgumentCaptor<ClientRequest> captor = ArgumentCaptor
				.forClass(ClientRequest.class);
		ReactorLoadBalancerExchangeFilterFunction filterFunction = new ReactorLoadBalancerExchangeFilterFunction(
				factory, Arrays.asList(new Transformer1(), new Transformer2()));
		filterFunction.filter(clientRequest, next).subscribe();
		verify(next).exchange(captor.capture());
		HttpHeaders headers = captor.getValue().headers();
		assertThat(headers.getFirst("X-ServiceId")).isEqualTo("testServiceId");
		assertThat(headers.getFirst("X-InstanceId")).isEqualTo("testServiceId");
	}

	@Test
	void transformRetryableLoadBalancerExchangeFilterFunction() {
		ArgumentCaptor<ClientRequest> captor = ArgumentCaptor
				.forClass(ClientRequest.class);
		RetryableLoadBalancerExchangeFilterFunction filterFunction = new RetryableLoadBalancerExchangeFilterFunction(
				policy, factory, properties,
				Arrays.asList(new Transformer1(), new Transformer2()));
		filterFunction.filter(clientRequest, next).subscribe();
		verify(next).exchange(captor.capture());
		HttpHeaders headers = captor.getValue().headers();
		assertThat(headers.getFirst("X-ServiceId")).isEqualTo("testServiceId");
		assertThat(headers.getFirst("X-InstanceId")).isEqualTo("testServiceId");
	}

	class Transformer1 implements LoadBalancerClientRequestTransformer {

		@Override
		public ClientRequest transformRequest(ClientRequest request,
				ServiceInstance instance) {
			return ClientRequest.from(request)
					.header("X-ServiceId", instance.getServiceId()).build();
		}

	}

	class Transformer2 implements LoadBalancerClientRequestTransformer {

		@Override
		public ClientRequest transformRequest(ClientRequest request,
				ServiceInstance instance) {
			return ClientRequest.from(request)
					.header("X-InstanceId", instance.getInstanceId()).build();
		}

	}

}
