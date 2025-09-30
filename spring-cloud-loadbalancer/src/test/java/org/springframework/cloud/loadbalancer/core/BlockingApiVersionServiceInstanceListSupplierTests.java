/*
 * Copyright 2025-present the original author or authors.
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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.accept.SemanticApiVersionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BlockingApiVersionServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 */
class BlockingApiVersionServiceInstanceListSupplierTests {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private final LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);

	private BlockingApiVersionServiceInstanceListSupplier supplier;

	private final LoadBalancerProperties properties = new LoadBalancerProperties();

	private final ServiceInstance first = serviceInstance("test-1", buildApiVersionMetadata("1"));

	private final ServiceInstance second = serviceInstance("test-2", buildApiVersionMetadata("2"));

	private final ServiceInstance third = serviceInstance("test-2", Collections.emptyMap());

	@BeforeEach
	void setUp() {
		when(loadBalancerClientFactory.getProperties(any())).thenReturn(properties);
		when(delegate.get()).thenReturn(Flux.just(Arrays.asList(first, second, third)));
		when(delegate.get(any())).thenReturn(Flux.just(Arrays.asList(first, second, third)));
		supplier = new BlockingApiVersionServiceInstanceListSupplier(delegate, loadBalancerClientFactory);
		supplier.setApiVersionParser(new SemanticApiVersionParser());
	}

	@Test
	void shouldFilterByRequestedVersion() {
		properties.getApiVersion().setHeader("X-Api-Version");
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Version", "1.0");
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://localhost/test"), headers,
				new LinkedMultiValueMap<>(), Collections.emptyMap());

		List<ServiceInstance> filtered = supplier.get(new DefaultRequest<>(new RequestDataContext(requestData)))
			.blockFirst();

		assertThat(filtered).containsExactly(first);
	}

	@Test
	void shouldReturnEmptyListWhenRequestedVersionIsNull() {
		properties.getApiVersion().setHeader("X-Api-Version");
		HttpHeaders headers = new HttpHeaders();
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://localhost/test"), headers,
				new LinkedMultiValueMap<>(), Collections.emptyMap());

		List<ServiceInstance> filtered = supplier.get(new DefaultRequest<>(new RequestDataContext(requestData)))
			.blockFirst();

		assertThat(filtered).isEmpty();
	}

	@Test
	void shouldReturnAllInstancesWhenRequestedVersionIsNullAndFallbackEnabled() {
		properties.getApiVersion().setHeader("X-Api-Version");
		properties.getApiVersion().setFallbackToAvailableInstances(true);
		HttpHeaders headers = new HttpHeaders();
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://localhost/test"), headers,
				new LinkedMultiValueMap<>(), Collections.emptyMap());

		List<ServiceInstance> filtered = supplier.get(new DefaultRequest<>(new RequestDataContext(requestData)))
			.blockFirst();

		assertThat(filtered).containsExactly(first, second, third);
	}

	@Test
	void shouldUseDefaultVersionWhenNoRequestData() {
		properties.getApiVersion().setHeader("X-Api-Version");
		properties.getApiVersion().setDefaultVersion("2");

		List<ServiceInstance> filtered = supplier.get().blockFirst();

		assertThat(filtered).containsExactly(second);
	}

	private DefaultServiceInstance serviceInstance(String instanceId, Map<String, String> metadata) {
		return new DefaultServiceInstance(instanceId, "test", "http://test.test", 9080, false, metadata);
	}

	private Map<String, String> buildApiVersionMetadata(String apiVersion) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put(BlockingApiVersionServiceInstanceListSupplier.API_VERSION, apiVersion);
		return metadata;
	}

}
