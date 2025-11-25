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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.InstanceProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Base class for {@link LoadBalancedExchangeFilterFunction} integration tests.
 *
 * @author Olga Maciaszek-Sharma
 * @author Haotian Zhang
 */
@SuppressWarnings("DataFlowIssue")
abstract class AbstractLoadBalancerExchangeFilterFunctionIntegrationTests {

	@Autowired
	protected LoadBalancedExchangeFilterFunction loadBalancerFunction;

	@Autowired
	protected SimpleDiscoveryProperties properties;

	@Autowired
	protected LoadBalancerProperties loadBalancerProperties;

	@Autowired
	protected ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	@LocalServerPort
	protected int port;

	@BeforeEach
	protected void setUp() {
		InstanceProperties instance = new InstanceProperties();
		instance.setServiceId("testservice");
		instance.setUri(URI.create("http://localhost:" + port));
		InstanceProperties instanceWithNoLifecycleProcessors = new InstanceProperties();
		instanceWithNoLifecycleProcessors.setServiceId("serviceWithNoLifecycleProcessors");
		instanceWithNoLifecycleProcessors.setUri(URI.create("http://localhost:" + port));
		properties.getInstances().put("testservice", Collections.singletonList(instance));
		properties.getInstances()
			.put("serviceWithNoLifecycleProcessors", Collections.singletonList(instanceWithNoLifecycleProcessors));
	}

	@Test
	void correctResponseReturnedForExistingHostAndInstancePresent() {
		ResponseEntity<String> response = WebClient.builder()
			.baseUrl("http://testservice")
			.filter(loadBalancerFunction)
			.build()
			.get()
			.uri("/hello")
			.retrieve()
			.toEntity(String.class)
			.block();
		then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(response.getBody()).isEqualTo("Hello World");
	}

	@Test
	void serviceUnavailableReturnedWhenNoInstancePresent() {
		assertThatIllegalStateException()
			.isThrownBy(() -> WebClient.builder()
				.baseUrl("http://xxx")
				.filter(loadBalancerFunction)
				.defaultStatusHandler(httpStatusCode -> httpStatusCode.equals(HttpStatus.SERVICE_UNAVAILABLE),
						clientResponse -> Mono.just(new IllegalStateException("503")))
				.build()
				.get()
				.retrieve()
				.toBodilessEntity()
				.block())
			.withMessage("503");
	}

	@Test
	void badRequestReturnedForIncorrectHost() {
		assertThatIllegalStateException()
			.isThrownBy(() -> WebClient.builder()
				.baseUrl("http:///xxx")
				.filter(loadBalancerFunction)
				.defaultStatusHandler(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
						response -> Mono.just(new IllegalStateException("400")))
				.build()
				.get()
				.retrieve()
				.toBodilessEntity()
				.block())
			.withMessage("400");
	}

	@Test
	void exceptionNotThrownWhenFactoryReturnsNullLifecycleProcessorsMap() {
		assertThatCode(() -> WebClient.builder()
			.baseUrl("http://serviceWithNoLifecycleProcessors")
			.filter(loadBalancerFunction)
			.build()
			.get()
			.uri("/hello")
			.exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
			.block()).doesNotThrowAnyException();
	}

	@Test
	void loadBalancerLifecycleCallbacksExecuted() {
		final String callbackTestHint = "callbackTestHint";
		loadBalancerProperties.getHint().put("testservice", "callbackTestHint");

		ResponseEntity<Void> response = WebClient.builder()
			.baseUrl("http://testservice")
			.filter(loadBalancerFunction)
			.build()
			.get()
			.uri("/callback")
			.retrieve()
			.toBodilessEntity()
			.block();

		Collection<Request<Object>> lifecycleLogRequests = ((TestLoadBalancerLifecycle) factory
			.getInstances("testservice", LoadBalancerLifecycle.class)
			.get("loadBalancerLifecycle")).getStartLog().values();
		Collection<Request<Object>> lifecycleLogStartRequests = ((TestLoadBalancerLifecycle) factory
			.getInstances("testservice", LoadBalancerLifecycle.class)
			.get("loadBalancerLifecycle")).getStartRequestLog().values();
		Collection<CompletionContext<Object, ServiceInstance, Object>> anotherLifecycleLogRequests = ((AnotherLoadBalancerLifecycle) factory
			.getInstances("testservice", LoadBalancerLifecycle.class)
			.get("anotherLoadBalancerLifecycle")).getCompleteLog().values();
		then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(lifecycleLogRequests).extracting(request -> ((DefaultRequestContext) request.getContext()).getHint())
			.contains(callbackTestHint);
		assertThat(lifecycleLogStartRequests)
			.extracting(request -> ((DefaultRequestContext) request.getContext()).getHint())
			.contains(callbackTestHint);
		assertThat(anotherLifecycleLogRequests)
			.extracting(completionContext -> ((ResponseData) completionContext.getClientResponse()).getRequestData()
				.getHttpMethod())
			.contains(HttpMethod.GET);
	}

	protected static class TestLoadBalancerFactory implements ReactiveLoadBalancer.Factory<ServiceInstance> {

		private final ReactorLoadBalancerExchangeFilterFunctionIntegrationTests.TestLoadBalancerLifecycle testLoadBalancerLifecycle;

		private final ReactorLoadBalancerExchangeFilterFunctionIntegrationTests.TestLoadBalancerLifecycle anotherLoadBalancerLifecycle;

		private final DiscoveryClient discoveryClient;

		private final LoadBalancerProperties properties;

		public TestLoadBalancerFactory(DiscoveryClient discoveryClient, LoadBalancerProperties properties) {
			this.discoveryClient = discoveryClient;
			this.properties = properties;
			testLoadBalancerLifecycle = new ReactorLoadBalancerExchangeFilterFunctionIntegrationTests.TestLoadBalancerLifecycle();
			anotherLoadBalancerLifecycle = new ReactorLoadBalancerExchangeFilterFunctionIntegrationTests.AnotherLoadBalancerLifecycle();
		}

		@Override
		public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
			return new DiscoveryClientBasedReactiveLoadBalancer(serviceId, discoveryClient);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public <X> Map<String, X> getInstances(String name, Class<X> type) {
			if (name.equals("serviceWithNoLifecycleProcessors")) {
				return null;
			}
			Map lifecycleProcessors = new HashMap<>();
			lifecycleProcessors.put("loadBalancerLifecycle", testLoadBalancerLifecycle);
			lifecycleProcessors.put("anotherLoadBalancerLifecycle", anotherLoadBalancerLifecycle);
			return lifecycleProcessors;
		}

		@Override
		public <X> X getInstance(String name, Class<?> clazz, Class<?>... generics) {
			return null;
		}

		@Override
		public LoadBalancerProperties getProperties(String serviceId) {
			return properties;
		}

	}

	protected static class TestLoadBalancerLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {

		final Map<String, Request<Object>> startLog = new ConcurrentHashMap<>();

		final Map<String, Request<Object>> startRequestLog = new ConcurrentHashMap<>();

		final Map<String, CompletionContext<Object, ServiceInstance, Object>> completeLog = new ConcurrentHashMap<>();

		@Override
		public void onStart(Request<Object> request) {
			startLog.put(getName() + UUID.randomUUID(), request);
		}

		@Override
		public void onStartRequest(Request<Object> request, Response<ServiceInstance> lbResponse) {
			startRequestLog.put(getName() + UUID.randomUUID(), request);
		}

		@Override
		public void onComplete(CompletionContext<Object, ServiceInstance, Object> completionContext) {
			completeLog.clear();
			completeLog.put(getName() + UUID.randomUUID(), completionContext);
		}

		Map<String, Request<Object>> getStartLog() {
			return startLog;
		}

		Map<String, CompletionContext<Object, ServiceInstance, Object>> getCompleteLog() {
			return completeLog;
		}

		Map<String, Request<Object>> getStartRequestLog() {
			return startRequestLog;
		}

		protected String getName() {
			return getClass().getSimpleName();
		}

	}

	protected static class AnotherLoadBalancerLifecycle extends TestLoadBalancerLifecycle {

	}

}
