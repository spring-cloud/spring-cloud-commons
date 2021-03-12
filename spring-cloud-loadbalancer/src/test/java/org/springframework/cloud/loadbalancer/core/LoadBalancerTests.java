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

package org.springframework.cloud.loadbalancer.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext;
import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext.Status;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceSuppliers;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class LoadBalancerTests {

	@Autowired
	private LoadBalancerClientFactory clientFactory;

	@Test
	public void roundRobbinLoadbalancerWorks() {
		ReactiveLoadBalancer<ServiceInstance> reactiveLoadBalancer = this.clientFactory
				.getInstance("myservice", ReactiveLoadBalancer.class,
						ServiceInstance.class);

		then(reactiveLoadBalancer).isInstanceOf(RoundRobinLoadBalancer.class);
		then(reactiveLoadBalancer).isInstanceOf(ReactorLoadBalancer.class);
		ReactorLoadBalancer<ServiceInstance> loadBalancer = (ReactorLoadBalancer<ServiceInstance>) reactiveLoadBalancer;

		// order dependent on seedPosition -1 of RoundRobinLoadBalancer
		List<String> hosts = Arrays.asList("ahost", "chost", "bhostsecure", "ahost");

		assertLoadBalancer(loadBalancer, hosts);
	}

	private void assertLoadBalancer(ReactorLoadBalancer<ServiceInstance> loadBalancer,
			List<String> hosts) {
		for (String host : hosts) {
			Mono<Response<ServiceInstance>> source = loadBalancer.choose();
			StepVerifier.create(source).consumeNextWith(response -> {
				then(response).isNotNull();
				then(response.hasServer()).isTrue();

				ServiceInstance instance = response.getServer();
				then(instance).isNotNull();
				then(instance.getHost()).as("instance host is incorrent %s", host)
						.isEqualTo(host);

				if (host.contains("secure")) {
					then(instance.isSecure()).isTrue();
				}
				else {
					then(instance.isSecure()).isFalse();
				}

				response.onComplete(new CompletionContext(Status.SUCCESSS));
			}).verifyComplete();
		}
	}

	@Test
	public void emptyHosts() {
		ResolvableType type = ResolvableType
				.forClassWithGenerics(ReactorLoadBalancer.class, ServiceInstance.class);
		ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory
				.getInstance("unknownservice", type);

		then(loadBalancer).isInstanceOf(RoundRobinLoadBalancer.class);

		Mono<Response<ServiceInstance>> source = loadBalancer.choose();
		StepVerifier.create(source).consumeNextWith(response -> {
			then(response).isNotNull();
			then(response.hasServer()).isFalse();
		}).verifyComplete();
	}

	@Test
	public void staticConfigurationWorks() {
		String serviceId = "test1";
		RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(serviceId,
				ServiceInstanceSuppliers.toProvider(serviceId,
						instance(serviceId, "1host", false),
						instance(serviceId, "2host-secure", true)),
				-1);
		assertLoadBalancer(loadBalancer, Arrays.asList("1host", "2host-secure"));
	}

	private static DefaultServiceInstance instance(String serviceId, String host,
			boolean secure) {
		return new DefaultServiceInstance(serviceId, serviceId, host, 80, secure);
	}

	@Test
	public void staticConfigurationWorksWithServiceInstanceListSupplier() {
		String serviceId = "test1";
		RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(
				ServiceInstanceListSuppliers.toProvider(serviceId,
						instance(serviceId, "1host", false),
						instance(serviceId, "2host-secure", true)),
				serviceId, -1);
		assertLoadBalancer(loadBalancer, Arrays.asList("1host", "2host-secure"));
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void canPassHintViaRequest() {
		String serviceId = "test1";
		RoundRobinLoadBalancer loadBalancer = new TestHintLoadBalancer(
				ServiceInstanceListSuppliers.toProvider(serviceId,
						instance(serviceId, "1host", false),
						instance(serviceId, "2host-secure", true)),
				serviceId);
		Request<DefaultRequestContext> request = new DefaultRequest<>(
				new DefaultRequestContext("test2"));

		ServiceInstance serviceInstance = loadBalancer.choose(request).block()
				.getServer();

		assertThat(serviceInstance.getServiceId()).isEqualTo("test2");
	}

	@Test
	public void selectedInstanceCallback() {
		String serviceId = "test1";
		ServiceInstance serviceInstance = instance(serviceId, "1host", false);
		SameInstancePreferenceServiceInstanceListSupplier supplier = mock(
				SameInstancePreferenceServiceInstanceListSupplier.class);
		when(supplier.get())
				.thenReturn(Flux.just(Collections.singletonList(serviceInstance)));
		RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(
				new SimpleObjectProvider<>(supplier), serviceId);

		loadBalancer.choose().block();

		verify(supplier).selectedServiceInstance(serviceInstance);
	}

	private static class TestHintLoadBalancer extends RoundRobinLoadBalancer {

		TestHintLoadBalancer(
				ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
				String serviceId) {
			super(serviceInstanceListSupplierProvider, serviceId);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Mono<Response<ServiceInstance>> choose(Request request) {
			if (request.getContext() instanceof DefaultRequestContext) {
				DefaultRequestContext requestContext = (DefaultRequestContext) request
						.getContext();
				return Mono.just(new DefaultResponse(
						instance(requestContext.getHint(), "host", false)));
			}
			return Mono.empty();
		}

	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@LoadBalancerClients({
			@LoadBalancerClient(name = "myservice",
					configuration = MyServiceConfig.class),
			@LoadBalancerClient(name = "unknownservice",
					configuration = MyServiceConfig.class) })
	@EnableCaching
	protected static class Config {

	}

	protected static class MyServiceConfig {

		@Bean
		public RoundRobinLoadBalancer roundRobinContextLoadBalancer(
				LoadBalancerClientFactory clientFactory, Environment env) {
			String serviceId = clientFactory.getName(env);
			return new RoundRobinLoadBalancer(clientFactory.getLazyProvider(serviceId,
					ServiceInstanceListSupplier.class), serviceId, -1);
		}

	}

}
