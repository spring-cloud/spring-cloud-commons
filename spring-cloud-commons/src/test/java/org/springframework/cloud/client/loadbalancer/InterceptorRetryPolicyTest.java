/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class InterceptorRetryPolicyTest {

	private HttpRequest request;

	private LoadBalancedRetryPolicy policy;

	private ServiceInstanceChooser serviceInstanceChooser;

	private String serviceName;

	@Before
	public void setup() {
		this.request = mock(HttpRequest.class);
		this.policy = mock(LoadBalancedRetryPolicy.class);
		this.serviceInstanceChooser = mock(ServiceInstanceChooser.class);
		this.serviceName = "foo";
	}

	@After
	public void teardown() {
		this.request = null;
		this.policy = null;
		this.serviceInstanceChooser = null;
		this.serviceName = null;
	}

	@Test
	public void canRetryBeforeExecution() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		when(context.getRetryCount()).thenReturn(0);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.serviceInstanceChooser.choose(eq(this.serviceName)))
				.thenReturn(serviceInstance);
		then(interceptorRetryPolicy.canRetry(context)).isTrue();
		verify(context, times(1)).setServiceInstance(eq(serviceInstance));

	}

	@Test
	public void canRetryNextServer() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		when(context.getRetryCount()).thenReturn(1);
		when(this.policy.canRetryNextServer(eq(context))).thenReturn(true);
		then(interceptorRetryPolicy.canRetry(context)).isTrue();
	}

	@Test
	public void cannotRetry() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		when(context.getRetryCount()).thenReturn(1);
		then(interceptorRetryPolicy.canRetry(context)).isFalse();
	}

	@Test
	public void open() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		RetryContext context = interceptorRetryPolicy.open(null);
		then(context).isInstanceOf(LoadBalancedRetryContext.class);
	}

	@Test
	public void close() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		interceptorRetryPolicy.close(context);
		verify(this.policy, times(1)).close(eq(context));
	}

	@Test
	public void registerThrowable() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		Throwable thrown = new Exception();
		interceptorRetryPolicy.registerThrowable(context, thrown);
		verify(context, times(1)).registerThrowable(eq(thrown));
		verify(this.policy, times(1)).registerThrowable(eq(context), eq(thrown));
	}

	@Test
	public void equals() throws Exception {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(
				this.request, this.policy, this.serviceInstanceChooser, this.serviceName);
		then(interceptorRetryPolicy.equals(null)).isFalse();
		then(interceptorRetryPolicy.equals(new Object())).isFalse();
		then(interceptorRetryPolicy.equals(interceptorRetryPolicy)).isTrue();
		then(interceptorRetryPolicy.equals(new InterceptorRetryPolicy(this.request,
				this.policy, this.serviceInstanceChooser, this.serviceName))).isTrue();
	}

}
