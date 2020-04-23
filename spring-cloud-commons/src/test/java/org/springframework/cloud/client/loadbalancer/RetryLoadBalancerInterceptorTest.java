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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.TerminatedRetryException;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 * @author Gang Li
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryLoadBalancerInterceptorTest {

	private LoadBalancerClient client;

	private LoadBalancerRetryProperties lbProperties;

	private LoadBalancerRequestFactory lbRequestFactory;

	private LoadBalancedRetryFactory loadBalancedRetryFactory = new LoadBalancedRetryFactory() {
	};

	@Before
	public void setUp() throws Exception {
		this.client = mock(LoadBalancerClient.class);
		this.lbProperties = new LoadBalancerRetryProperties();
		this.lbRequestFactory = mock(LoadBalancerRequestFactory.class);

	}

	@After
	public void tearDown() throws Exception {
		this.client = null;
		this.lbProperties = null;
	}

	@Test(expected = IOException.class)
	public void interceptDisableRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenThrow(new IOException());
		this.lbProperties.setEnabled(false);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				this.loadBalancedRetryFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));

		interceptor.intercept(request, body, execution);
		verify(this.lbRequestFactory).createRequest(request, body, execution);
	}

	@Test(expected = IllegalStateException.class)
	public void interceptInvalidHost() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo_underscore"));
		this.lbProperties.setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				this.loadBalancedRetryFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		interceptor.intercept(request, body, execution);
	}

	@Test
	public void interceptNeverRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {},
				HttpStatus.OK);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));
		this.lbProperties.setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				this.loadBalancedRetryFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		interceptor.intercept(request, body, execution);
		verify(this.lbRequestFactory).createRequest(request, body, execution);
	}

	@Test
	public void interceptSuccess() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {},
				HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));
		this.lbProperties.setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy));
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		then(rsp).isEqualTo(clientHttpResponse);
		verify(this.lbRequestFactory).createRequest(request, body, execution);
	}

	@Test
	public void interceptRetryOnStatusCode() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		InputStream notFoundStream = mock(InputStream.class);
		when(notFoundStream.read(any(byte[].class))).thenReturn(-1);
		ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(
				notFoundStream, HttpStatus.NOT_FOUND);
		ClientHttpResponse clientHttpResponseOk = new MockClientHttpResponse(
				new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value())))
				.thenReturn(true);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class)))
				.thenReturn(true);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				nullable(LoadBalancerRequest.class)))
						.thenReturn(clientHttpResponseNotFound)
						.thenReturn(clientHttpResponseOk);
		this.lbProperties.setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy));
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(this.client, times(2)).execute(eq("foo"), eq(serviceInstance),
				nullable(LoadBalancerRequest.class));
		verify(notFoundStream, times(1)).close();
		then(rsp).isEqualTo(clientHttpResponseOk);
		verify(this.lbRequestFactory, times(2)).createRequest(request, body, execution);
	}

	@Test
	public void interceptRetryFailOnStatusCode() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));

		InputStream notFoundStream = new ByteArrayInputStream("foo".getBytes());
		ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(
				notFoundStream, HttpStatus.NOT_FOUND);

		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value())))
				.thenReturn(true);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class)))
				.thenReturn(false);

		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any()))
						.thenReturn(clientHttpResponseNotFound);

		this.lbProperties.setEnabled(true);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy));
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);

		verify(this.client, times(1)).execute(eq("foo"), eq(serviceInstance),
				ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any());
		verify(this.lbRequestFactory, times(1)).createRequest(request, body, execution);
		verify(policy, times(2)).canRetryNextServer(any(LoadBalancedRetryContext.class));

		// call twice in a retry attempt
		byte[] content = new byte[1024];
		int length = rsp.getBody().read(content);
		then(length).isEqualTo("foo".getBytes().length);
		then(new String(content, 0, length)).isEqualTo("foo");
	}

	@Test
	public void interceptRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {},
				HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class)))
				.thenReturn(true);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenThrow(new IOException())
						.thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));
		this.lbProperties.setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy));
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(this.client, times(2)).execute(eq("foo"), eq(serviceInstance),
				any(LoadBalancerRequest.class));
		then(rsp).isEqualTo(clientHttpResponse);
		verify(this.lbRequestFactory, times(2)).createRequest(request, body, execution);
		then(backOffPolicy.getBackoffAttempts()).isEqualTo(1);
	}

	@Test(expected = IOException.class)
	public void interceptFailedRetry() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {},
				HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class)))
				.thenReturn(false);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("foo"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("foo"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenThrow(new IOException())
						.thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));
		this.lbProperties.setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy));
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		interceptor.intercept(request, body, execution);
		verify(this.lbRequestFactory).createRequest(request, body, execution);
	}

	@Test
	public void retryListenerTest() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://listener"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {},
				HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class)))
				.thenReturn(true);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("listener"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("listener"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenThrow(new IOException())
						.thenReturn(clientHttpResponse);
		this.lbProperties.setEnabled(true);
		MyRetryListener retryListener = new MyRetryListener();
		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy,
						new RetryListener[] { retryListener }));
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(this.client, times(2)).execute(eq("listener"), eq(serviceInstance),
				any(LoadBalancerRequest.class));
		then(rsp).isEqualTo(clientHttpResponse);
		verify(this.lbRequestFactory, times(2)).createRequest(request, body, execution);
		then(backOffPolicy.getBackoffAttempts()).isEqualTo(1);
		then(retryListener.getOnError()).isEqualTo(1);
	}

	@Test(expected = TerminatedRetryException.class)
	public void retryListenerTestNoRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://noRetry"));
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		this.lbProperties.setEnabled(true);
		RetryListener myRetryListener = new RetryListenerSupport() {
			@Override
			public <T, E extends Throwable> boolean open(RetryContext context,
					RetryCallback<T, E> callback) {
				return false;
			}
		};
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy,
						new RetryListener[] { myRetryListener }));
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		interceptor.intercept(request, new byte[] {}, execution);
	}

	@Test
	public void retryWithDefaultConstructorTest() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://default"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {},
				HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class)))
				.thenReturn(true);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(this.client.choose(eq("default"))).thenReturn(serviceInstance);
		when(this.client.execute(eq("default"), eq(serviceInstance),
				any(LoadBalancerRequest.class))).thenThrow(new IOException())
						.thenReturn(clientHttpResponse);
		this.lbProperties.setEnabled(true);
		when(this.lbRequestFactory.createRequest(any(), any(), any()))
				.thenReturn(mock(LoadBalancerRequest.class));
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(
				this.client, this.lbProperties, this.lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy));
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(this.client, times(2)).execute(eq("default"), eq(serviceInstance),
				any(LoadBalancerRequest.class));
		then(rsp).isEqualTo(clientHttpResponse);
		verify(this.lbRequestFactory, times(2)).createRequest(request, body, execution);
		then(backOffPolicy.getBackoffAttempts()).isEqualTo(1);
	}

	class MyLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

		private LoadBalancedRetryPolicy loadBalancedRetryPolicy;

		private BackOffPolicy backOffPolicy;

		private RetryListener[] retryListeners;

		MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy) {
			this.loadBalancedRetryPolicy = loadBalancedRetryPolicy;
		}

		MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy,
				BackOffPolicy backOffPolicy) {
			this(loadBalancedRetryPolicy);
			this.backOffPolicy = backOffPolicy;
		}

		MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy,
				BackOffPolicy backOffPolicy, RetryListener[] retryListeners) {
			this(loadBalancedRetryPolicy, backOffPolicy);
			this.retryListeners = retryListeners;
		}

		@Override
		public LoadBalancedRetryPolicy createRetryPolicy(String service,
				ServiceInstanceChooser serviceInstanceChooser) {
			return this.loadBalancedRetryPolicy;
		}

		@Override
		public BackOffPolicy createBackOffPolicy(String service) {
			if (this.backOffPolicy == null) {
				return new NoBackOffPolicy();
			}
			else {
				return this.backOffPolicy;
			}
		}

		@Override
		public RetryListener[] createRetryListeners(String service) {
			if (this.retryListeners == null) {
				return new RetryListener[0];
			}
			else {
				return this.retryListeners;
			}
		}

	}

	class MyBackOffPolicy implements BackOffPolicy {

		private int backoffAttempts = 0;

		@Override
		public BackOffContext start(RetryContext retryContext) {
			return new BackOffContext() {
				@Override
				protected Object clone() throws CloneNotSupportedException {
					return super.clone();
				}
			};
		}

		@Override
		public void backOff(BackOffContext backOffContext)
				throws BackOffInterruptedException {
			this.backoffAttempts++;
		}

		int getBackoffAttempts() {
			return this.backoffAttempts;
		}

	}

	class MyRetryListener extends RetryListenerSupport {

		private int onError = 0;

		@Override
		public <T, E extends Throwable> void onError(RetryContext retryContext,
				RetryCallback<T, E> retryCallback, Throwable throwable) {
			this.onError++;
		}

		int getOnError() {
			return this.onError;
		}

	}

}
