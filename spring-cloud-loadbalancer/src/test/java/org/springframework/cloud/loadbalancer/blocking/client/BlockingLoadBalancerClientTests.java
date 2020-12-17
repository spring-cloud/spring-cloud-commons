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

package org.springframework.cloud.loadbalancer.blocking.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link BlockingLoadBalancerClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 */
@SpringBootTest
class BlockingLoadBalancerClientTests {

	@Autowired
	private BlockingLoadBalancerClient loadBalancerClient;

	@Autowired
	private SimpleDiscoveryProperties properties;

	@Autowired
	ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	@Autowired
	LoadBalancerProperties loadBalancerProperties;

	@BeforeEach
	void setUp() {
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(null, null, "test.example", 9999, true);
		properties.getInstances().put("myservice", Collections.singletonList(serviceInstance));
	}

	@Test
	void correctServiceInstanceChosen() {
		ServiceInstance serviceInstance = loadBalancerClient.choose("myservice");
		assertThat(serviceInstance.getHost()).isEqualTo("test.example");
	}

	@Test
	void nullReturnedIfInstanceMissing() {
		ServiceInstance serviceInstance = loadBalancerClient.choose("unknownservice");
		assertThat(serviceInstance).isNull();
	}

	@Test
	void requestExecutedAgainstCorrectInstance() throws IOException {
		final String result = "result";
		Object actualResult = loadBalancerClient.execute("myservice", (LoadBalancerRequest<Object>) instance -> {
			assertThat(instance.getHost()).isEqualTo("test.example");
			return result;
		});
		assertThat(actualResult).isEqualTo(result);
	}

	@Test
	void exceptionThrownIfInstanceNotAvailableForRequestExecution() {
		try {
			final String result = "result";
			Object actualResult = loadBalancerClient.execute("unknownservice",
					(LoadBalancerRequest<Object>) instance -> result);
			assertThat(actualResult).isEqualTo(result);
			fail("Should have thrown exception.");
		}
		catch (Exception exception) {
			assertThat(exception).isNotNull();
			assertThat(exception).isInstanceOf(IllegalStateException.class);
			assertThat(exception).hasMessage("No instances available for unknownservice");
		}
	}

	@Test
	void exceptionRethrownAsRuntime() {
		try {
			loadBalancerClient.execute("myservice", instance -> {
				assertThat(instance.getHost()).isEqualTo("test.example");
				throw new Exception("Should throw exception.");
			});
			fail("Should have thrown exception.");
		}
		catch (Exception exception) {
			assertThat(exception).isNotNull();
			assertThat(exception).isInstanceOf(RuntimeException.class);
		}
	}

	@Test
	void IOExceptionRethrown() {
		try {
			loadBalancerClient.execute("myservice", instance -> {
				assertThat(instance.getHost()).isEqualTo("test.example");
				throw new IOException("Should throw IO exception.");
			});
			fail("Should have thrown exception.");
		}
		catch (Exception exception) {
			assertThat(exception).isNotNull();
			assertThat(exception).isInstanceOf(IOException.class);
		}
	}

	@Test
	void exceptionNotThrownWhenFactoryReturnsNullLifecycleProcessorsMap() {
		assertThatCode(() -> loadBalancerClient.execute("serviceWithNoLifecycleProcessors",
				(LoadBalancerRequest<Object>) instance -> {
					assertThat(instance.getHost()).isEqualTo("test.example");
					return "result";
				})).doesNotThrowAnyException();
	}

	@Test
	void loadBalancerLifecycleCallbacksExecuted() throws IOException {
		String callbackTestHint = "callbackTestHint";
		loadBalancerProperties.getHint().put("myservice", "callbackTestHint");
		final String result = "callbackTestResult";
		Object actualResult = loadBalancerClient.execute("myservice", (LoadBalancerRequest<Object>) instance -> {
			assertThat(instance.getHost()).isEqualTo("test.example");
			return result;
		});

		Collection<Request<Object>> lifecycleLogRequests = ((TestLoadBalancerLifecycle) factory
				.getInstances("myservice", LoadBalancerLifecycle.class).get("loadBalancerLifecycle")).getStartLog()
						.values();
		Collection<Request<Object>> lifecycleLogStartedRequests = ((TestLoadBalancerLifecycle) factory
				.getInstances("myservice", LoadBalancerLifecycle.class).get("loadBalancerLifecycle"))
						.getStartRequestLog().values();
		Collection<CompletionContext<Object, ServiceInstance, Object>> anotherLifecycleLogRequests = ((AnotherLoadBalancerLifecycle) factory
				.getInstances("myservice", LoadBalancerLifecycle.class).get("anotherLoadBalancerLifecycle"))
						.getCompleteLog().values();
		assertThat(actualResult).isEqualTo(result);
		assertThat(lifecycleLogRequests).extracting(request -> ((DefaultRequestContext) request.getContext()).getHint())
				.contains(callbackTestHint);
		assertThat(lifecycleLogStartedRequests)
				.extracting(request -> ((DefaultRequestContext) request.getContext()).getHint())
				.contains(callbackTestHint);
		assertThat(anotherLifecycleLogRequests).extracting(CompletionContext::getClientResponse).contains(result);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@LoadBalancerClients({ @LoadBalancerClient(name = "myservice", configuration = MyServiceConfig.class),
			@LoadBalancerClient(name = "unknownservice", configuration = UnknownServiceConfig.class),
			@LoadBalancerClient(name = "serviceWithNoLifecycleProcessors",
					configuration = NoLifecycleProcessorsConfig.class) })
	protected static class Config {

	}

	protected static class NoLifecycleProcessorsConfig {

		@Bean
		ReactorLoadBalancer<ServiceInstance> reactiveLoadBalancer(DiscoveryClient discoveryClient) {
			return new DiscoveryClientBasedReactiveLoadBalancer("myservice", discoveryClient);
		}

	}

	protected static class MyServiceConfig {

		@Bean
		ReactorLoadBalancer<ServiceInstance> reactiveLoadBalancer(DiscoveryClient discoveryClient) {
			return new DiscoveryClientBasedReactiveLoadBalancer("myservice", discoveryClient);
		}

		@Bean
		LoadBalancerLifecycle<Object, Object, ServiceInstance> loadBalancerLifecycle() {
			return new TestLoadBalancerLifecycle();
		}

		@Bean
		LoadBalancerLifecycle<Object, Object, ServiceInstance> anotherLoadBalancerLifecycle() {
			return new AnotherLoadBalancerLifecycle();
		}

	}

	protected static class UnknownServiceConfig {

		@Bean
		ReactorLoadBalancer<ServiceInstance> reactiveLoadBalancer(DiscoveryClient discoveryClient) {
			return new DiscoveryClientBasedReactiveLoadBalancer("unknownservice", discoveryClient);
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

@SuppressWarnings("rawtypes")
class DiscoveryClientBasedReactiveLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private final Random random = new Random();

	private final String serviceId;

	private final DiscoveryClient discoveryClient;

	DiscoveryClientBasedReactiveLoadBalancer(String serviceId, DiscoveryClient discoveryClient) {
		this.serviceId = serviceId;
		this.discoveryClient = discoveryClient;
	}

	@Override
	public Mono<Response<ServiceInstance>> choose() {
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		if (instances.size() == 0) {
			return Mono.just(new EmptyResponse());
		}
		int instanceIdx = this.random.nextInt(instances.size());
		return Mono.just(new DefaultResponse(instances.get(instanceIdx)));
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		return choose();
	}

}
