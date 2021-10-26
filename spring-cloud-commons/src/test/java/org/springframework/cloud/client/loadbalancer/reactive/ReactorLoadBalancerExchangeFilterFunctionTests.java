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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for {@link ReactorLoadBalancerExchangeFilterFunction}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 */
@SuppressWarnings("ConstantConditions")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ReactorLoadBalancerExchangeFilterFunctionTests {

	@Autowired
	private ReactorLoadBalancerExchangeFilterFunction loadBalancerFunction;

	@Autowired
	private SimpleDiscoveryProperties properties;

	@Autowired
	private LoadBalancerProperties loadBalancerProperties;

	@Autowired
	private ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	@LocalServerPort
	private int port;

	@BeforeEach
	void setUp() {
		DefaultServiceInstance instance = new DefaultServiceInstance();
		instance.setServiceId("testservice");
		instance.setUri(URI.create("http://localhost:" + this.port));
		DefaultServiceInstance instanceWithNoLifecycleProcessors = new DefaultServiceInstance();
		instanceWithNoLifecycleProcessors.setServiceId("serviceWithNoLifecycleProcessors");
		instanceWithNoLifecycleProcessors.setUri(URI.create("http://localhost:" + this.port));
		properties.getInstances().put("testservice", Collections.singletonList(instance));
		properties.getInstances().put("serviceWithNoLifecycleProcessors",
				Collections.singletonList(instanceWithNoLifecycleProcessors));
	}

	@Test
	void correctResponseReturnedForExistingHostAndInstancePresent() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://testservice").filter(loadBalancerFunction)
				.build().get().uri("/hello").exchange().block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
		then(clientResponse.bodyToMono(String.class).block()).isEqualTo("Hello World");
	}

	@Test
	void serviceUnavailableReturnedWhenNoInstancePresent() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://xxx").filter(this.loadBalancerFunction)
				.build().get().exchange().block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	@Disabled // FIXME 3.0.0
	void badRequestReturnedForIncorrectHost() {
		ClientResponse clientResponse = WebClient.builder().baseUrl("http:///xxx").filter(this.loadBalancerFunction)
				.build().get().exchange().block();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void exceptionNotThrownWhenFactoryReturnsNullLifecycleProcessorsMap() {
		assertThatCode(() -> WebClient.builder().baseUrl("http://serviceWithNoLifecycleProcessors")
				.filter(loadBalancerFunction).build().get().uri("/hello").exchange().block())
						.doesNotThrowAnyException();
	}

	@Test
	void loadBalancerLifecycleCallbacksExecuted() {
		final String callbackTestHint = "callbackTestHint";
		loadBalancerProperties.getHint().put("testservice", "callbackTestHint");
		ClientResponse clientResponse = WebClient.builder().baseUrl("http://testservice").filter(loadBalancerFunction)
				.build().get().uri("/callback").exchange().block();

		Collection<Request<Object>> lifecycleLogRequests = ((TestLoadBalancerLifecycle) factory
				.getInstances("testservice", LoadBalancerLifecycle.class).get("loadBalancerLifecycle")).getStartLog()
						.values();
		Collection<Request<Object>> lifecycleStartedLogRequests = ((TestLoadBalancerLifecycle) factory
				.getInstances("testservice", LoadBalancerLifecycle.class).get("loadBalancerLifecycle"))
						.getStartRequestLog().values();
		Collection<CompletionContext<Object, ServiceInstance, Object>> anotherLifecycleLogRequests = ((AnotherLoadBalancerLifecycle) factory
				.getInstances("testservice", LoadBalancerLifecycle.class).get("anotherLoadBalancerLifecycle"))
						.getCompleteLog().values();
		then(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(lifecycleLogRequests).extracting(request -> ((DefaultRequestContext) request.getContext()).getHint())
				.contains(callbackTestHint);
		assertThat(lifecycleStartedLogRequests)
				.extracting(request -> ((DefaultRequestContext) request.getContext()).getHint())
				.contains(callbackTestHint);
		assertThat(anotherLifecycleLogRequests)
				.extracting(completionContext -> ((ResponseData) completionContext.getClientResponse()).getRequestData()
						.getUrl().toString())
				.contains("http://testservice/callback");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@EnableDiscoveryClient
	@EnableAutoConfiguration
	@SpringBootConfiguration(proxyBeanMethods = false)
	@RestController
	static class Config {

		@GetMapping("/hello")
		public String hello() {
			return "Hello World";
		}

		@GetMapping("/callback")
		String callbackTestResult() {
			return "callbackTestResult";
		}

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> reactiveLoadBalancerFactory(DiscoveryClient discoveryClient,
				LoadBalancerProperties properties) {
			return new ReactiveLoadBalancer.Factory<ServiceInstance>() {

				private final TestLoadBalancerLifecycle testLoadBalancerLifecycle = new TestLoadBalancerLifecycle();

				private final TestLoadBalancerLifecycle anotherLoadBalancerLifecycle = new AnotherLoadBalancerLifecycle();

				@Override
				public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
					return new DiscoveryClientBasedReactiveLoadBalancer(serviceId, discoveryClient);
				}

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
			};
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
			return this.getClass().getSimpleName();
		}

	}

	protected static class AnotherLoadBalancerLifecycle extends TestLoadBalancerLifecycle {

		@Override
		protected String getName() {
			return this.getClass().getSimpleName();
		}

	}

}
