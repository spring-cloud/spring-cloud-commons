/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.cloud.loadbalancer.stats;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMapAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.UNKNOWN;

/**
 * Tests for {@link MicrometerStatsLoadBalancerLifecycle}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Jaroslaw Dembek
 */
class MicrometerStatsLoadBalancerLifecycleTests {

	private static final String WEB_CLIENT_URI_TEMPLATE_ATTRIBUTE = "org.springframework.web.reactive.function.client.WebClient.uriTemplate";

	private static final String REST_CLIENT_URI_TEMPLATE_ATTRIBUTE = "org.springframework.web.reactive.function.client.WebClient.uriTemplate";

	MeterRegistry meterRegistry = new SimpleMeterRegistry();

	MicrometerStatsLoadBalancerLifecycle statsLifecycle = new MicrometerStatsLoadBalancerLifecycle(meterRegistry);

	@Test
	void shouldRecordSuccessfulTimedRequest() {
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://test.org/test"), new HttpHeaders(),
				new HttpHeaders(), new HashMap<>());
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData));
		Response<ServiceInstance> lbResponse = new DefaultResponse(
				new DefaultServiceInstance("test-1", "test", "test.org", 8080, false, new HashMap<>()));
		ResponseData responseData = new ResponseData(HttpStatus.OK, new HttpHeaders(),
				new MultiValueMapAdapter<>(new HashMap<>()), requestData);
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle
			.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest, lbResponse, responseData));

		assertThat(meterRegistry.getMeters()).hasSize(2);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(0);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timers()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().count()).isEqualTo(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().getId().getTags()).contains(
				Tag.of("method", "GET"), Tag.of("outcome", "SUCCESS"), Tag.of("serviceId", "test"),
				Tag.of("serviceInstance.host", "test.org"), Tag.of("serviceInstance.instanceId", "test-1"),
				Tag.of("serviceInstance.port", "8080"), Tag.of("status", "200"), Tag.of("uri", "/test"));
	}

	@Test
	void shouldNotAddPathValueWhenDisabled() {
		ReactiveLoadBalancer.Factory<ServiceInstance> factory = mock(ReactiveLoadBalancer.Factory.class);
		LoadBalancerProperties properties = new LoadBalancerProperties();
		properties.getStats().setIncludePath(false);
		when(factory.getProperties("test")).thenReturn(properties);
		MicrometerStatsLoadBalancerLifecycle statsLifecycle = new MicrometerStatsLoadBalancerLifecycle(meterRegistry,
				factory);
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://test.org/test"), new HttpHeaders(),
				new HttpHeaders(), new HashMap<>());
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData));
		Response<ServiceInstance> lbResponse = new DefaultResponse(
				new DefaultServiceInstance("test-1", "test", "test.org", 8080, false, new HashMap<>()));
		ResponseData responseData = new ResponseData(HttpStatus.OK, new HttpHeaders(),
				new MultiValueMapAdapter<>(new HashMap<>()), requestData);
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle
			.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest, lbResponse, responseData));

		assertThat(meterRegistry.getMeters()).hasSize(2);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().getId().getTags())
			.doesNotContain(Tag.of("uri", "/test"));
	}

	@ParameterizedTest
	@ValueSource(strings = { WEB_CLIENT_URI_TEMPLATE_ATTRIBUTE, REST_CLIENT_URI_TEMPLATE_ATTRIBUTE })
	void shouldRecordSuccessfulTimedRequestWithUriTemplate(String attributeName) {
		Map<String, Object> attributes = new HashMap<>();
		String uriTemplate = "/test/{pathParam}/test";
		attributes.put(attributeName, uriTemplate);
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://test.org/test/123/test"),
				new HttpHeaders(), new HttpHeaders(), attributes);
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData));
		Response<ServiceInstance> lbResponse = new DefaultResponse(
				new DefaultServiceInstance("test-1", "test", "test.org", 8080, false, new HashMap<>()));
		ResponseData responseData = new ResponseData(HttpStatus.OK, new HttpHeaders(),
				new MultiValueMapAdapter<>(new HashMap<>()), requestData);
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle
			.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest, lbResponse, responseData));

		assertThat(meterRegistry.getMeters()).hasSize(2);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(0);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timers()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().count()).isEqualTo(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().getId().getTags())
			.containsExactlyInAnyOrder(Tag.of("method", "GET"), Tag.of("outcome", "SUCCESS"),
					Tag.of("serviceId", "test"), Tag.of("serviceInstance.host", "test.org"),
					Tag.of("serviceInstance.instanceId", "test-1"), Tag.of("serviceInstance.port", "8080"),
					Tag.of("status", "200"), Tag.of("uri", uriTemplate));
	}

	@Test
	void shouldRecordFailedTimedRequest() {
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://test.org/test"), new HttpHeaders(),
				new HttpHeaders(), new HashMap<>());
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData));
		Response<ServiceInstance> lbResponse = new DefaultResponse(
				new DefaultServiceInstance("test-1", "test", "test.org", 8080, false, new HashMap<>()));
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.FAILED, new IllegalStateException(),
				lbRequest, lbResponse));

		assertThat(meterRegistry.getMeters()).hasSize(2);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(0);
		assertThat(meterRegistry.get("loadbalancer.requests.failed").timers()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.failed").timer().count()).isEqualTo(1);
		assertThat(meterRegistry.get("loadbalancer.requests.failed").timer().getId().getTags()).contains(
				Tag.of("exception", "IllegalStateException"), Tag.of("method", "GET"), Tag.of("serviceId", "test"),
				Tag.of("serviceInstance.host", "test.org"), Tag.of("serviceInstance.instanceId", "test-1"),
				Tag.of("serviceInstance.port", "8080"), Tag.of("uri", "/test"));
	}

	@Test
	void shouldNotRecordDiscardedRequest() {
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://test.org/test"), new HttpHeaders(),
				new HttpHeaders(), new HashMap<>());
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData));
		Response<ServiceInstance> lbResponse = new EmptyResponse();
		statsLifecycle.onStartRequest(lbRequest, lbResponse);

		statsLifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, lbResponse));
		assertThat(meterRegistry.getMeters()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.discard").counter().count()).isEqualTo(1);
	}

	@Test
	void shouldNotRecordUnTimedRequest() {
		Request<Object> lbRequest = new DefaultRequest<>(new StatsTestContext());
		Response<ServiceInstance> lbResponse = new DefaultResponse(
				new DefaultServiceInstance("test-1", "test", "test.org", 8080, false, new HashMap<>()));
		ResponseData responseData = new ResponseData(HttpStatus.OK, new HttpHeaders(),
				new MultiValueMapAdapter<>(new HashMap<>()), null);
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle
			.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest, lbResponse, responseData));

		assertThat(meterRegistry.getMeters()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(0);
	}

	@Test
	void shouldNotCreateNullTagsWhenNullDataObjects() {
		Request<Object> lbRequest = new DefaultRequest<>(new DefaultRequestContext());
		Response<ServiceInstance> lbResponse = new DefaultResponse(new DefaultServiceInstance());
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle
			.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest, lbResponse, null));

		assertThat(meterRegistry.getMeters()).hasSize(2);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(0);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timers()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().count()).isEqualTo(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().getId().getTags()).contains(
				Tag.of("method", UNKNOWN), Tag.of("outcome", UNKNOWN), Tag.of("serviceId", UNKNOWN),
				Tag.of("serviceInstance.host", UNKNOWN), Tag.of("serviceInstance.instanceId", UNKNOWN),
				Tag.of("serviceInstance.port", "0"), Tag.of("status", UNKNOWN), Tag.of("uri", UNKNOWN));
	}

	@Test
	void shouldNotCreateNullTagsWhenEmptyDataObjects() {
		RequestData requestData = new RequestData(null, null, null, null, null);
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext());
		Response<ServiceInstance> lbResponse = new DefaultResponse(new DefaultServiceInstance());
		ResponseData responseData = new ResponseData(null, null, null, requestData);
		statsLifecycle.onStartRequest(lbRequest, lbResponse);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(1);

		statsLifecycle
			.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest, lbResponse, responseData));

		assertThat(meterRegistry.getMeters()).hasSize(2);
		assertThat(meterRegistry.get("loadbalancer.requests.active").gauge().value()).isEqualTo(0);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timers()).hasSize(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().count()).isEqualTo(1);
		assertThat(meterRegistry.get("loadbalancer.requests.success").timer().getId().getTags()).contains(
				Tag.of("method", UNKNOWN), Tag.of("outcome", "SUCCESS"), Tag.of("serviceId", UNKNOWN),
				Tag.of("serviceInstance.host", UNKNOWN), Tag.of("serviceInstance.instanceId", UNKNOWN),
				Tag.of("serviceInstance.port", "0"), Tag.of("status", "200"), Tag.of("uri", UNKNOWN));
	}

	@Test
	void shouldHandleNullLoadBalancerResponse() {
		RequestData requestData = new RequestData(HttpMethod.GET, URI.create("http://test.org/test"), new HttpHeaders(),
				new HttpHeaders(), new HashMap<>());
		Request<Object> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData));
		assertThatCode(() -> {
			statsLifecycle.onStartRequest(lbRequest, null);
			statsLifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, null));
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldHandleNullLoadBalancerRequest() {
		Response<ServiceInstance> lbResponse = new EmptyResponse();
		assertThatCode(() -> {
			statsLifecycle.onStartRequest(null, lbResponse);
			statsLifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, null, lbResponse));
		}).doesNotThrowAnyException();
	}

	private static class StatsTestContext {

	}

}
