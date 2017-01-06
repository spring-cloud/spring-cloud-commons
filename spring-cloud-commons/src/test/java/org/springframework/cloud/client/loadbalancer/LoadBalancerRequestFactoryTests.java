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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalancerRequestFactoryTests {

	@Mock
	private LoadBalancerClient loadBalancer;
	@Mock
	private HttpRequest request;
	@Mock
	private HttpRequest transformedRequest1;
	@Mock
	private HttpRequest transformedRequest2;

	private byte[] body = new byte[] {};

	@Mock
	private ClientHttpRequestExecution execution;
	@Mock
	private ServiceInstance instance;
	@Mock
	private LoadBalancerRequestTransformer transformer1;
	@Mock
	private LoadBalancerRequestTransformer transformer2;

	private ArgumentCaptor<HttpRequest> httpRequestCaptor;

	@Before
	public void setup() {
		httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
	}

	@Test
	public void testNullTransformers() throws Exception {
		executeLbRequest(null);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		Assert.assertEquals("request should be of type ServiceRequestWrapper", ServiceRequestWrapper.class,
				httpRequestCaptor.getValue().getClass());
	}

	@Test
	public void testEmptyTransformers() throws Exception {
		List<LoadBalancerRequestTransformer> transformers = Collections.emptyList();

		executeLbRequest(transformers);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		Assert.assertEquals("request should be of type ServiceRequestWrapper", ServiceRequestWrapper.class,
				httpRequestCaptor.getValue().getClass());
	}

	@Test
	public void testOneTransformer() throws Exception {
		List<LoadBalancerRequestTransformer> transformers = Arrays.asList(transformer1);
		when(transformer1.transformRequest(any(ServiceRequestWrapper.class), eq(instance))).thenReturn(transformedRequest1);

		executeLbRequest(transformers);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals("transformer1 should have transformed request into transformedRequest1", transformedRequest1,
				httpRequestCaptor.getValue());
	}

	@Test
	public void testTwoTransformers() throws Exception {
		List<LoadBalancerRequestTransformer> transformers = Arrays.asList(transformer1, transformer2);
		when(transformer1.transformRequest(any(ServiceRequestWrapper.class), eq(instance))).thenReturn(transformedRequest1);
		when(transformer2.transformRequest(transformedRequest1, instance))
				.thenReturn(transformedRequest2);

		executeLbRequest(transformers);

		verify(execution).execute(httpRequestCaptor.capture(), eq(body));
		assertEquals("transformer2 should have transformed transformedRequest1 into transformedRequest2",
				transformedRequest2,
				httpRequestCaptor.getValue());
	}

	private void executeLbRequest(List<LoadBalancerRequestTransformer> transformers) throws Exception {
		LoadBalancerRequestFactory lbReqFactory = new LoadBalancerRequestFactory(loadBalancer, transformers);
		LoadBalancerRequest<ClientHttpResponse> lbRequest = lbReqFactory.createRequest(request, body, execution);
		lbRequest.apply(instance);
	}

}
