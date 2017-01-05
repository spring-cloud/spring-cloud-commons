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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
	private ClientHttpRequestExecution execution;
	@Mock
	private ServiceInstance instance;

	private byte[] body = new byte[] {};
	private ArgumentCaptor<HttpRequest> httpRequestCaptor;
	private LoadBalancerClient loadBalancerClient;
	private LoadBalancerRequestFactory lbReqFactory;
	private LoadBalancerRequest<?> lbRequest;

	@Before
	public void setup() {
		httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().web(false)
				.properties("spring.aop.proxyTargetClass=true")
				.sources(config, LoadBalancerAutoConfiguration.class).run();

		loadBalancerClient = context.getBean(LoadBalancerClient.class);
		lbReqFactory = context.getBean(LoadBalancerRequestFactory.class);
		lbRequest = lbReqFactory.createRequest(request, body, execution);
		return context;
	}

	@Test
	public void customTransformerRunsAfterUriReconstructingRequestTransformer() throws Exception {
		ConfigurableApplicationContext context = init(CustomTransformerRunsAfter.class);

		LoadBalancerRequestTransformer customTransformer = context.getBean("customTransformer",
				LoadBalancerRequestTransformer.class);
		when(customTransformer.transformRequest(any(UriReconstructingRequestWrapper.class), eq(instance),
				eq(loadBalancerClient))).thenReturn(transformedRequest);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"customTransformer should have transformed the already transformed ServiceRequestWrapper into transformedRequest",
				transformedRequest,
				httpRequestCaptor.getValue());
	}

	@Test
	public void customTransformerRunsBeforeUriReconstructingRequestTransformer() throws Exception {
		ConfigurableApplicationContext context = init(CustomTransformerRunsBefore.class);

		LoadBalancerRequestTransformer customTransformer = context.getBean("customTransformer",
				LoadBalancerRequestTransformer.class);
		when(customTransformer.transformRequest(request, instance, loadBalancerClient)).thenReturn(transformedRequest);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"UriReconstructingRequestWrapper should be executed because UriReconstructingRequestTransformer should run last",
				UriReconstructingRequestWrapper.class,
				httpRequestCaptor.getValue().getClass());
	}

	@Test
	public void uriReconstructingRequestTransformerOnly() throws Exception {
		init(NoCustomTransformer.class);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"UriReconstructingRequestWrapper should be executed because UriReconstructingRequestTransformer should run",
				UriReconstructingRequestWrapper.class,
				httpRequestCaptor.getValue().getClass());
	}

	@Test
	public void customTransformerOnly() throws Exception {
		ConfigurableApplicationContext context = init(OnlyCustomTransformer.class);

		LoadBalancerRequestTransformer customTransformer = context.getBean("customTransformer",
				LoadBalancerRequestTransformer.class);
		when(customTransformer.transformRequest(request, instance, loadBalancerClient)).thenReturn(transformedRequest);

		lbRequest.apply(instance);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals(
				"transformedRequest should be executed because its the only defined behaviour",
				transformedRequest,
				httpRequestCaptor.getValue());
	}

	@Configuration
	static class CustomTransformerRunsAfter {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

		@Bean
		@Order(LoadBalancerRequestTransformer.DEFAULT_ORDER + 1)
		public LoadBalancerRequestTransformer customTransformer() {
			return mock(LoadBalancerRequestTransformer.class);
		}

	}

	@Configuration
	static class CustomTransformerRunsBefore {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

		@Bean
		@Order(LoadBalancerRequestTransformer.DEFAULT_ORDER - 1)
		public LoadBalancerRequestTransformer customTransformer() {
			return mock(LoadBalancerRequestTransformer.class);
		}

	}

	@Configuration
	static class NoCustomTransformer {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

	}

	@Configuration
	static class OnlyCustomTransformer {

		@Bean
		public LoadBalancerClient loadBalancerClient() {
			return mock(LoadBalancerClient.class);
		}

		@Bean
		public LoadBalancerRequestTransformer customTransformer() {
			return mock(LoadBalancerRequestTransformer.class);
		}

		@Bean
		public LoadBalancerRequestFactory requestFactory() {
			return new LoadBalancerRequestFactory(loadBalancerClient(), Arrays.asList(customTransformer()));
		}

	}

}
