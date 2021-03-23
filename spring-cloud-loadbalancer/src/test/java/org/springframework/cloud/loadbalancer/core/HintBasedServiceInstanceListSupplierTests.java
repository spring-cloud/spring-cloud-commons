/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HintBasedServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
class HintBasedServiceInstanceListSupplierTests {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private final LoadBalancerProperties properties = new LoadBalancerProperties();

	private final RequestDataContext requestContext = new RequestDataContext(
			new RequestData(new MockClientHttpRequest()));

	private final HintBasedServiceInstanceListSupplier supplier = new HintBasedServiceInstanceListSupplier(delegate,
			properties);

	private final ServiceInstance first = serviceInstance("test-1", buildHintMetadata("test1"));

	private final ServiceInstance second = serviceInstance("test-2", buildHintMetadata("test2"));

	private final ServiceInstance third = serviceInstance("test-3", new HashMap<>());

	@BeforeEach
	void setUp() {
		properties.setHintHeaderName("X-Test");
		when(delegate.get(any())).thenReturn(Flux.just(Arrays.asList(first, second, third)));
	}

	@Test
	void shouldReturnInstancesForHintFromHeaderWhenAvailable() {
		requestContext.setHint("test1");
		requestContext.getClientRequest().getHeaders().add("X-Test", "test2");
		Request<RequestDataContext> request = new DefaultRequest<>(requestContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(1);
		assertThat(filtered.get(0).getInstanceId()).isEqualTo("test-2");
	}

	@Test
	void shouldReturnInstancesForHintFromPropertiesWhenNoHintHeader() {
		requestContext.setHint("test1");
		Request<RequestDataContext> request = new DefaultRequest<>(requestContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(1);
		assertThat(filtered.get(0).getInstanceId()).isEqualTo("test-1");
	}

	@Test
	void shouldReturnAllInstancesWhenNoHint() {
		Request<RequestDataContext> request = new DefaultRequest<>(requestContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(3);
	}

	@Test
	void shouldReturnAllInstancesWhenHintNotMatched() {
		requestContext.getClientRequest().getHeaders().add("X-Test", "testX");
		Request<RequestDataContext> request = new DefaultRequest<>(requestContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(3);
	}

	@Test
	void shouldReturnAllInstancesWhenRequestContextNull() {
		Request<RequestDataContext> request = new DefaultRequest<>(null);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(3);
	}

	@Test
	void shouldReturnAllInstancesWhenClientRequestNull() {
		Request<RequestDataContext> request = new DefaultRequest<>(new RequestDataContext(null));

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(3);
	}

	private DefaultServiceInstance serviceInstance(String instanceId, Map<String, String> metadata) {
		return new DefaultServiceInstance(instanceId, "test", "http://test.test", 9080, false, metadata);
	}

	private Map<String, String> buildHintMetadata(String zone) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("hint", zone);
		return metadata;
	}

}
