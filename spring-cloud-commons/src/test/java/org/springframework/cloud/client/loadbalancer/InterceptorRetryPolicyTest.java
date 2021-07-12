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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(MockitoExtension.class)
public class InterceptorRetryPolicyTest {

	private HttpRequest request;

	private LoadBalancedRetryPolicy policy;

	private ServiceInstanceChooser serviceInstanceChooser;

	private String serviceName;

	@BeforeEach
	public void setup() {
		request = mock(HttpRequest.class);
		policy = mock(LoadBalancedRetryPolicy.class);
		serviceInstanceChooser = mock(ServiceInstanceChooser.class);
		serviceName = "foo";
	}

	@AfterEach
	public void teardown() {
		request = null;
		policy = null;
		serviceInstanceChooser = null;
		serviceName = null;
	}

	@Test
	public void canRetryBeforeExecution() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		when(context.getRetryCount()).thenReturn(0);
		then(interceptorRetryPolicy.canRetry(context)).isTrue();
		verify(context, times(1)).setServiceInstance(eq(null));

	}

	@Test
	public void canRetryNextServer() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		when(context.getRetryCount()).thenReturn(1);
		when(policy.canRetryNextServer(eq(context))).thenReturn(true);
		then(interceptorRetryPolicy.canRetry(context)).isTrue();
	}

	@Test
	public void cannotRetry() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		when(context.getRetryCount()).thenReturn(1);
		then(interceptorRetryPolicy.canRetry(context)).isFalse();
	}

	@Test
	public void open() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		RetryContext context = interceptorRetryPolicy.open(null);
		then(context).isInstanceOf(LoadBalancedRetryContext.class);
	}

	@Test
	public void close() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		interceptorRetryPolicy.close(context);
		verify(policy, times(1)).close(eq(context));
	}

	@Test
	public void registerThrowable() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
		Throwable thrown = new Exception();
		interceptorRetryPolicy.registerThrowable(context, thrown);
		verify(context, times(1)).registerThrowable(eq(thrown));
		verify(policy, times(1)).registerThrowable(eq(context), eq(thrown));
	}

	@Test
	public void equals() {
		InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy,
				serviceInstanceChooser, serviceName);
		then(interceptorRetryPolicy.equals(null)).isFalse();
		then(interceptorRetryPolicy.equals(new Object())).isFalse();
		then(interceptorRetryPolicy.equals(interceptorRetryPolicy)).isTrue();
		then(interceptorRetryPolicy
				.equals(new InterceptorRetryPolicy(request, policy, serviceInstanceChooser, serviceName))).isTrue();
	}

}
