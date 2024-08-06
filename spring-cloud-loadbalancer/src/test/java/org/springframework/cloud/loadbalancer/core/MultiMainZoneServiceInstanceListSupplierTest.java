/*
 * Copyright 2012-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.config.LoadBalancerMultiMainZoneConfig;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MultiMainZoneServiceInstanceListSupplier}.
 *
 * @author seal
 */
public class MultiMainZoneServiceInstanceListSupplierTest {

	private final DiscoveryClientServiceInstanceListSupplier delegate = mock(
			DiscoveryClientServiceInstanceListSupplier.class);

	private final LoadBalancerMultiMainZoneConfig multiMainZoneConfig = new LoadBalancerMultiMainZoneConfig("DAILY", "zone");

	private MultiMainZoneServiceInstanceListSupplier supplier;

	private final LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);

	private final ClientRequest request = mock(ClientRequest.class);

	private final ServiceInstance first = serviceInstance("test-1", buildZoneMetadata("DAILY", "DAILY"));

	private final ServiceInstance second = serviceInstance("test-2", buildZoneMetadata("DAILY", "DAILY"));

	private final ServiceInstance third = serviceInstance("test-3", buildZoneMetadata("DAILY", "GRAY_1"));

	private final ServiceInstance fourth = serviceInstance("test-4", buildZoneMetadata("DAILY", "GRAY_2"));

	private final ServiceInstance fifth = serviceInstance("test-5", buildZoneMetadata("PRE", "PRE"));

	private final ServiceInstance sixth = serviceInstance("test-6", buildZoneMetadata(null, null));

	@BeforeEach
	void setUp() {
		LoadBalancerProperties properties = new LoadBalancerProperties();
		when(loadBalancerClientFactory.getProperties(any())).thenReturn(properties);
		supplier = new MultiMainZoneServiceInstanceListSupplier(delegate, multiMainZoneConfig, loadBalancerClientFactory);

		when(request.method()).thenReturn(HttpMethod.GET);
		when(request.url()).thenReturn(URI.create("https://spring.io"));

	}

	@Test
	void noZoneRequestHeaderKeyShouldOnlyMainZoneInstancesByMultiMainZone() {

		multiMainZoneConfig.setMainZone("DAILY");
		multiMainZoneConfig.setZoneRequestHeaderKey("zone");
		when(delegate.get(any())).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth, fifth, sixth)));

		HttpHeaders headers = new HttpHeaders();
		when(request.headers()).thenReturn(headers);

		RequestData requestData = new RequestData(request);
		RequestDataContext requestDataContext = new RequestDataContext(requestData);
		Request<RequestDataContext> request = new DefaultRequest<>(requestDataContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(2);
		assertThat(filtered).contains(first, second);
		assertThat(filtered).doesNotContain(third);
		assertThat(filtered).doesNotContain(fourth);
		assertThat(filtered).doesNotContain(fifth);
		assertThat(filtered).doesNotContain(sixth);
	}

	@Test
	void mainZoneRequestHeaderKeyShouldOnlyMainZoneInstancesByMultiMainZone() {

		multiMainZoneConfig.setMainZone("DAILY");
		multiMainZoneConfig.setZoneRequestHeaderKey("zone");
		when(delegate.get(any())).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth, fifth, sixth)));

		HttpHeaders headers = new HttpHeaders();
		headers.add("zone", "DAILY");
		when(request.headers()).thenReturn(headers);

		RequestData requestData = new RequestData(request);
		RequestDataContext requestDataContext = new RequestDataContext(requestData);
		Request<RequestDataContext> request = new DefaultRequest<>(requestDataContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(2);
		assertThat(filtered).contains(first, second);
		assertThat(filtered).doesNotContain(third);
		assertThat(filtered).doesNotContain(fourth);
		assertThat(filtered).doesNotContain(fifth);
		assertThat(filtered).doesNotContain(sixth);
	}

	@Test
	void gray1ZoneRequestHeaderKeyShouldOnlyMainZoneInstancesByMultiMainZone() {

		multiMainZoneConfig.setMainZone("DAILY");
		multiMainZoneConfig.setZoneRequestHeaderKey("zone");
		when(delegate.get(any())).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth, fifth, sixth)));

		HttpHeaders headers = new HttpHeaders();
		headers.add("zone", "GRAY_1");
		when(request.headers()).thenReturn(headers);

		RequestData requestData = new RequestData(request);
		RequestDataContext requestDataContext = new RequestDataContext(requestData);
		Request<RequestDataContext> request = new DefaultRequest<>(requestDataContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(1);
		assertThat(filtered).doesNotContain(first);
		assertThat(filtered).doesNotContain(second);
		assertThat(filtered).contains(third);
		assertThat(filtered).doesNotContain(fourth);
		assertThat(filtered).doesNotContain(fifth);
		assertThat(filtered).doesNotContain(sixth);
	}

	@Test
	void preZoneRequestHeaderKeyShouldOnlyMainZoneInstancesByMultiMainZone() {

		multiMainZoneConfig.setMainZone("PRE");
		multiMainZoneConfig.setZoneRequestHeaderKey("zone");
		when(delegate.get(any())).thenReturn(Flux.just(Arrays.asList(first, second, third, fourth, fifth, sixth)));

		HttpHeaders headers = new HttpHeaders();
//		headers.add("zone", "GRAY_1");
		when(request.headers()).thenReturn(headers);

		RequestData requestData = new RequestData(request);
		RequestDataContext requestDataContext = new RequestDataContext(requestData);
		Request<RequestDataContext> request = new DefaultRequest<>(requestDataContext);

		List<ServiceInstance> filtered = supplier.get(request).blockFirst();

		assertThat(filtered).hasSize(1);
		assertThat(filtered).doesNotContain(first);
		assertThat(filtered).doesNotContain(second);
		assertThat(filtered).doesNotContain(third);
		assertThat(filtered).doesNotContain(fourth);
		assertThat(filtered).contains(fifth);
		assertThat(filtered).doesNotContain(sixth);
	}

	private DefaultServiceInstance serviceInstance(String instanceId, Map<String, String> metadata) {
		return new DefaultServiceInstance(instanceId, "test", "http://test.test", 9080, false, metadata);
	}

	private Map<String, String> buildZoneMetadata(String mainZone, String subsetZone) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("main-zone", mainZone);
		metadata.put("subset-zone", subsetZone);
		return metadata;
	}
}
