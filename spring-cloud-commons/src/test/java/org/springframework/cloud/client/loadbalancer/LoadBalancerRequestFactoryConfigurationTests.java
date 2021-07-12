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

package org.springframework.cloud.client.loadbalancer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerRequestFactoryConfigurationTests {

	@Mock
	private HttpRequest request;

	@Mock
	private HttpRequest transformedRequest;

	@Mock
	private HttpRequest transformedRequest2;

	@Mock
	private ClientHttpRequestExecution execution;

	@Mock
	private ServiceInstance instance;

	private byte[] body = new byte[] {};

	private ArgumentCaptor<HttpRequest> httpRequestCaptor;

	private LoadBalancerRequestFactory lbReqFactory;

	private LoadBalancerRequest<?> lbRequest;

	@BeforeEach
	public void setup() {
		this.httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.aop.proxyTargetClass=true").sources(config, LoadBalancerAutoConfiguration.class)
				.run();

		this.lbReqFactory = context.getBean(LoadBalancerRequestFactory.class);
		this.lbRequest = this.lbReqFactory.createRequest(this.request, this.body, this.execution);
		return context;
	}

	@Test
	public void transformer() throws Exception {
		ConfigurableApplicationContext context = init(Transformer.class);

		LoadBalancerRequestTransformer transformer = context.getBean("transformer",
				LoadBalancerRequestTransformer.class);
		when(transformer.transformRequest(any(ServiceRequestWrapper.class), eq(this.instance)))
				.thenReturn(this.transformedRequest);

		this.lbRequest.apply(this.instance);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue())
				.as("transformer should have transformed the ServiceRequestWrapper into transformedRequest")
				.isEqualTo(this.transformedRequest);
	}

	@Test
	public void noTransformer() throws Exception {
		init(NoTransformer.class);

		this.lbRequest.apply(this.instance);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue().getClass()).as("ServiceRequestWrapper should be executed")
				.isEqualTo(ServiceRequestWrapper.class);
	}

	@Test
	public void transformersAreOrdered() throws Exception {
		ConfigurableApplicationContext context = init(TransformersAreOrdered.class);

		LoadBalancerRequestTransformer transformer = context.getBean("transformer",
				LoadBalancerRequestTransformer.class);
		when(transformer.transformRequest(any(ServiceRequestWrapper.class), eq(this.instance)))
				.thenReturn(this.transformedRequest);
		LoadBalancerRequestTransformer transformer2 = context.getBean("transformer2",
				LoadBalancerRequestTransformer.class);
		when(transformer2.transformRequest(this.transformedRequest, this.instance))
				.thenReturn(this.transformedRequest2);

		this.lbRequest.apply(this.instance);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue()).as("transformer2 should run after transformer")
				.isEqualTo(this.transformedRequest2);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(NoTransformer.class)
	static class Transformer {

		@Bean
		public LoadBalancerRequestTransformer transformer() {
			return mock(LoadBalancerRequestTransformer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Transformer.class)
	static class TransformersAreOrdered {

		@Bean
		@Order(LoadBalancerRequestTransformer.DEFAULT_ORDER + 1)
		public LoadBalancerRequestTransformer transformer2() {
			return mock(LoadBalancerRequestTransformer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NoTransformer {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

		@SuppressWarnings("unchecked")
		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> factory() {
			return mock(ReactiveLoadBalancer.Factory.class);
		}

	}

}
