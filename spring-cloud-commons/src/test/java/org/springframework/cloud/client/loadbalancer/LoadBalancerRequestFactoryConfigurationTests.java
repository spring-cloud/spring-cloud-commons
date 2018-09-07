/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.client.loadbalancer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

@RunWith(MockitoJUnitRunner.class)
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

	@Before
	public void setup() {
		httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.aop.proxyTargetClass=true")
				.sources(config, LoadBalancerAutoConfiguration.class).run();

		lbReqFactory = context.getBean(LoadBalancerRequestFactory.class);
		lbRequest = lbReqFactory.createRequest(request, body, execution);
		return context;
	}

	@Test
	public void transformer() throws Exception {
		ConfigurableApplicationContext context = init(Transformer.class);

		LoadBalancerRequestTransformer transformer = context.getBean("transformer",
				LoadBalancerRequestTransformer.class);
		when(transformer.transformRequest(any(ServiceRequestWrapper.class), eq(instance)))
				.thenReturn(transformedRequest);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"transformer should have transformed the ServiceRequestWrapper into transformedRequest",
				transformedRequest,
				httpRequestCaptor.getValue());
	}

	@Test
	public void noTransformer() throws Exception {
		init(NoTransformer.class);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"ServiceRequestWrapper should be executed",
				ServiceRequestWrapper.class,
				httpRequestCaptor.getValue().getClass());
	}

	@Test
	public void transformersAreOrdered() throws Exception {
		ConfigurableApplicationContext context = init(TransformersAreOrdered.class);

		LoadBalancerRequestTransformer transformer = context.getBean("transformer",
				LoadBalancerRequestTransformer.class);
		when(transformer.transformRequest(any(ServiceRequestWrapper.class), eq(instance)))
				.thenReturn(transformedRequest);
		LoadBalancerRequestTransformer transformer2 = context.getBean("transformer2",
				LoadBalancerRequestTransformer.class);
		when(transformer2.transformRequest(transformedRequest, instance)).thenReturn(transformedRequest2);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"transformer2 should run after transformer",
				transformedRequest2,
				httpRequestCaptor.getValue());
	}

	@Configuration
	static class Transformer {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

		@Bean
		public LoadBalancerRequestTransformer transformer() {
			return mock(LoadBalancerRequestTransformer.class);
		}

	}

	@Configuration
	static class TransformersAreOrdered {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

		@Bean
		public LoadBalancerRequestTransformer transformer() {
			return mock(LoadBalancerRequestTransformer.class);
		}

		@Bean
		@Order(LoadBalancerRequestTransformer.DEFAULT_ORDER + 1)
		public LoadBalancerRequestTransformer transformer2() {
			return mock(LoadBalancerRequestTransformer.class);
		}

	}

	@Configuration
	static class NoTransformer {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

	}

}
