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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
	public void setup() {
		this.httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
	}

	@Test
	public void testNullTransformers() throws Exception {
		executeLbRequest(null);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue().getClass()).as("request should be of type ServiceRequestWrapper")
				.isEqualTo(ServiceRequestWrapper.class);
	}

	@Test
	public void testEmptyTransformers() throws Exception {
		List<LoadBalancerRequestTransformer> transformers = Collections.emptyList();

		executeLbRequest(transformers);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue().getClass()).as("request should be of type ServiceRequestWrapper")
				.isEqualTo(ServiceRequestWrapper.class);
	}

	@Test
	public void testOneTransformer() throws Exception {
		List<LoadBalancerRequestTransformer> transformers = Arrays.asList(this.transformer1);
		when(this.transformer1.transformRequest(any(ServiceRequestWrapper.class), eq(this.instance)))
				.thenReturn(this.transformedRequest1);

		executeLbRequest(transformers);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue())
				.as("transformer1 should have transformed request into transformedRequest1")
				.isEqualTo(this.transformedRequest1);
	}

	@Test
	public void testTwoTransformers() throws Exception {
		List<LoadBalancerRequestTransformer> transformers = Arrays.asList(this.transformer1, this.transformer2);
		when(this.transformer1.transformRequest(any(ServiceRequestWrapper.class), eq(this.instance)))
				.thenReturn(this.transformedRequest1);
		when(this.transformer2.transformRequest(this.transformedRequest1, this.instance))
				.thenReturn(this.transformedRequest2);

		executeLbRequest(transformers);

		verify(this.execution).execute(this.httpRequestCaptor.capture(), eq(this.body));
		then(this.httpRequestCaptor.getValue())
				.as("transformer2 should have transformed transformedRequest1 into transformedRequest2")
				.isEqualTo(this.transformedRequest2);
	}

	private void executeLbRequest(List<LoadBalancerRequestTransformer> transformers) throws Exception {
		LoadBalancerRequestFactory lbReqFactory = new LoadBalancerRequestFactory(this.loadBalancer, transformers);
		LoadBalancerRequest<ClientHttpResponse> lbRequest = lbReqFactory.createRequest(this.request, this.body,
				this.execution);
		lbRequest.apply(this.instance);
	}

}
