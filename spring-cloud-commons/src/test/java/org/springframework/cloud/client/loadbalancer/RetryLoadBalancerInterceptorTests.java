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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author Ryan Baxter
 * @author Gang Li
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@ExtendWith(MockitoExtension.class)
public class RetryLoadBalancerInterceptorTests {

	private LoadBalancerClient client;

	private LoadBalancerRequestFactory lbRequestFactory;

	private final LoadBalancedRetryFactory loadBalancedRetryFactory = new LoadBalancedRetryFactory() {
	};

	private LoadBalancerProperties properties;

	private ReactiveLoadBalancer.Factory<ServiceInstance> lbFactory;

	@BeforeEach
	public void setUp() {
		client = mock(LoadBalancerClient.class);
		lbRequestFactory = mock(LoadBalancerRequestFactory.class);
		properties = new LoadBalancerProperties();
		lbFactory = mock(ReactiveLoadBalancer.Factory.class, withSettings().lenient());
		when(lbFactory.getProperties(any())).thenReturn(properties);
	}

	@AfterEach
	public void tearDown() {
		client = null;
	}

	@Test
	public void interceptDisableRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenThrow(new IOException());
		properties.getRetry().setEnabled(false);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				loadBalancedRetryFactory, lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));

		Assertions.assertThrows(IOException.class, () -> {
			interceptor.intercept(request, body, execution);
		});
		verify(lbRequestFactory).createRequest(request, body, execution);
	}

	@Test
	public void interceptInvalidHost() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo_underscore"));
		properties.getRetry().setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				loadBalancedRetryFactory, lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		Assertions.assertThrows(IllegalStateException.class, () -> {
			interceptor.intercept(request, body, execution);
		});
	}

	@Test
	public void interceptNeverRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenReturn(clientHttpResponse);
		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
		properties.getRetry().setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				loadBalancedRetryFactory, lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		interceptor.intercept(request, body, execution);
		verify(lbRequestFactory).createRequest(request, body, execution);
	}

	@Test
	public void interceptSuccess() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenReturn(clientHttpResponse);
		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
		properties.getRetry().setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy), lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		then(rsp).isEqualTo(clientHttpResponse);
		verify(lbRequestFactory).createRequest(request, body, execution);
	}

	@Test
	public void interceptRetryOnStatusCode() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		InputStream notFoundStream = mock(InputStream.class);
		when(notFoundStream.read(any(byte[].class))).thenReturn(-1);
		ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(notFoundStream,
				HttpStatus.NOT_FOUND);
		ClientHttpResponse clientHttpResponseOk = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value()))).thenReturn(true);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance), nullable(LoadBalancerRequest.class)))
				.thenReturn(clientHttpResponseNotFound).thenReturn(clientHttpResponseOk);
		properties.getRetry().setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy), lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), nullable(LoadBalancerRequest.class));
		verify(notFoundStream, times(1)).close();
		then(rsp).isEqualTo(clientHttpResponseOk);
		verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
	}

	@Test
	public void interceptRetryFailOnStatusCode() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));

		InputStream notFoundStream = new ByteArrayInputStream("foo".getBytes());
		ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(notFoundStream,
				HttpStatus.NOT_FOUND);

		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value()))).thenReturn(true);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(false);

		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance),
				ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any()))
						.thenReturn(clientHttpResponseNotFound);

		properties.getRetry().setEnabled(true);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy), lbFactory);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);

		verify(client, times(1)).execute(eq("foo"), eq(serviceInstance),
				ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any());
		verify(lbRequestFactory, times(1)).createRequest(request, body, execution);
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
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
		properties.getRetry().setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy), lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class));
		then(rsp).isEqualTo(clientHttpResponse);
		verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
		then(backOffPolicy.getBackoffAttempts()).isEqualTo(1);
	}

	@Test
	public void interceptFailedRetry() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://foo"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(false);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("foo"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
		properties.getRetry().setEnabled(true);
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy), lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		Assertions.assertThrows(IOException.class, () -> {
			interceptor.intercept(request, body, execution);
		});
		verify(lbRequestFactory).createRequest(request, body, execution);
	}

	private static ServiceInstance defaultServiceInstance() {
		return new DefaultServiceInstance("testInstance", "test", "testHost", 80, false);
	}

	@Test
	public void retryListenerTest() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://listener"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("listener"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("listener"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenThrow(new IOException()).thenReturn(clientHttpResponse);
		properties.getRetry().setEnabled(true);
		MyRetryListener retryListener = new MyRetryListener();
		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy, new RetryListener[] { retryListener }),
				lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(client, times(2)).execute(eq("listener"), eq(serviceInstance), any(LoadBalancerRequest.class));
		then(rsp).isEqualTo(clientHttpResponse);
		verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
		then(backOffPolicy.getBackoffAttempts()).isEqualTo(1);
		then(retryListener.getOnError()).isEqualTo(1);
	}

	@Test
	public void retryWithDefaultConstructorTest() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://default"));
		ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		when(client.choose(eq("default"), any())).thenReturn(serviceInstance);
		when(client.execute(eq("default"), eq(serviceInstance), any(LoadBalancerRequest.class)))
				.thenThrow(new IOException()).thenReturn(clientHttpResponse);
		properties.getRetry().setEnabled(true);
		when(lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy), lbFactory);
		byte[] body = new byte[] {};
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
		verify(client, times(2)).execute(eq("default"), eq(serviceInstance), any(LoadBalancerRequest.class));
		then(rsp).isEqualTo(clientHttpResponse);
		verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
		then(backOffPolicy.getBackoffAttempts()).isEqualTo(1);
	}

	@Test
	public void retryListenerTestNoRetry() throws Throwable {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://noRetry"));
		LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
		MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
		properties.getRetry().setEnabled(true);
		RetryListener myRetryListener = new RetryListenerSupport() {
			@Override
			public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
				return false;
			}
		};
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				new MyLoadBalancedRetryFactory(policy, backOffPolicy, new RetryListener[] { myRetryListener }),
				lbFactory);
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		Assertions.assertThrows(TerminatedRetryException.class, () -> {
			interceptor.intercept(request, new byte[] {}, execution);
		});
	}

	@Test
	public void shouldNotDuplicateLifecycleCalls() throws IOException, URISyntaxException {
		Map<String, LoadBalancerLifecycle> lifecycleProcessors = new HashMap<>();
		lifecycleProcessors.put("testLifecycle", new TestLoadBalancerLifecycle());
		lifecycleProcessors.put("anotherLifecycle", new AnotherLoadBalancerLifecycle());
		when(lbFactory.getInstances("test", LoadBalancerLifecycle.class)).thenReturn(lifecycleProcessors);
		HttpRequest request = mock(HttpRequest.class);
		when(request.getURI()).thenReturn(new URI("http://test"));
		TestLoadBalancerClient client = new TestLoadBalancerClient();
		RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbRequestFactory,
				loadBalancedRetryFactory, lbFactory);

		interceptor.intercept(request, new byte[] {}, mock(ClientHttpRequestExecution.class));

		assertThat(((TestLoadBalancerLifecycle) lifecycleProcessors.get("testLifecycle")).getStartLog()).hasSize(1);
		assertThat(((TestLoadBalancerLifecycle) lifecycleProcessors.get("testLifecycle")).getStartRequestLog())
				.hasSize(0);
		assertThat(((TestLoadBalancerLifecycle) lifecycleProcessors.get("testLifecycle")).getCompleteLog()).hasSize(0);
		assertThat(((TestLoadBalancerLifecycle) lifecycleProcessors.get("anotherLifecycle")).getStartLog()).hasSize(1);
		assertThat(((TestLoadBalancerLifecycle) lifecycleProcessors.get("anotherLifecycle")).getStartRequestLog())
				.hasSize(0);
		assertThat(((TestLoadBalancerLifecycle) lifecycleProcessors.get("anotherLifecycle")).getCompleteLog())
				.hasSize(0);
		assertThat(((TestLoadBalancerLifecycle) client.getLifecycleProcessors().get("testLifecycle")).getStartLog())
				.hasSize(0);
		assertThat(
				((TestLoadBalancerLifecycle) client.getLifecycleProcessors().get("testLifecycle")).getStartRequestLog())
						.hasSize(1);
		assertThat(((TestLoadBalancerLifecycle) client.getLifecycleProcessors().get("testLifecycle")).getCompleteLog())
				.hasSize(1);
		assertThat(((TestLoadBalancerLifecycle) client.getLifecycleProcessors().get("anotherLifecycle")).getStartLog())
				.hasSize(0);
		assertThat(
				((TestLoadBalancerLifecycle) client.getLifecycleProcessors().get("testLifecycle")).getStartRequestLog())
						.hasSize(1);
		assertThat(
				((TestLoadBalancerLifecycle) client.getLifecycleProcessors().get("anotherLifecycle")).getCompleteLog())
						.hasSize(1);
	}

	static class MyLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

		private final LoadBalancedRetryPolicy loadBalancedRetryPolicy;

		private BackOffPolicy backOffPolicy;

		private RetryListener[] retryListeners;

		MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy) {
			this.loadBalancedRetryPolicy = loadBalancedRetryPolicy;
		}

		MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy, BackOffPolicy backOffPolicy) {
			this(loadBalancedRetryPolicy);
			this.backOffPolicy = backOffPolicy;
		}

		MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy, BackOffPolicy backOffPolicy,
				RetryListener[] retryListeners) {
			this(loadBalancedRetryPolicy, backOffPolicy);
			this.retryListeners = retryListeners;
		}

		@Override
		public LoadBalancedRetryPolicy createRetryPolicy(String service,
				ServiceInstanceChooser serviceInstanceChooser) {
			return loadBalancedRetryPolicy;
		}

		@Override
		public BackOffPolicy createBackOffPolicy(String service) {
			if (backOffPolicy == null) {
				return new NoBackOffPolicy();
			}
			else {
				return backOffPolicy;
			}
		}

		@Override
		public RetryListener[] createRetryListeners(String service) {
			if (retryListeners == null) {
				return new RetryListener[0];
			}
			else {
				return retryListeners;
			}
		}

	}

	static class MyBackOffPolicy implements BackOffPolicy {

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
		public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
			backoffAttempts++;
		}

		int getBackoffAttempts() {
			return backoffAttempts;
		}

	}

	static class MyRetryListener extends RetryListenerSupport {

		private int onError = 0;

		@Override
		public <T, E extends Throwable> void onError(RetryContext retryContext, RetryCallback<T, E> retryCallback,
				Throwable throwable) {
			onError++;
		}

		int getOnError() {
			return onError;
		}

	}

	protected static class TestLoadBalancerClient implements LoadBalancerClient {

		private final Map<String, LoadBalancerLifecycle> lifecycleProcessors = new HashMap<>();

		TestLoadBalancerClient() {
			lifecycleProcessors.put("testLifecycle", new TestLoadBalancerLifecycle());
			lifecycleProcessors.put("anotherLifecycle", new AnotherLoadBalancerLifecycle());
		}

		@Override
		public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) {
			Set<LoadBalancerLifecycle> supportedLoadBalancerProcessors = LoadBalancerLifecycleValidator
					.getSupportedLifecycleProcessors(lifecycleProcessors, DefaultRequestContext.class, Object.class,
							ServiceInstance.class);
			supportedLoadBalancerProcessors.forEach(lifecycle -> lifecycle.onStartRequest(new DefaultRequest<>(),
					new DefaultResponse(serviceInstance)));
			T response = (T) new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
			supportedLoadBalancerProcessors
					.forEach(lifecycle -> lifecycle.onComplete(new CompletionContext(CompletionContext.Status.SUCCESS,
							new DefaultRequest<>(), new DefaultResponse(defaultServiceInstance()))));
			return response;
		}

		@Override
		public URI reconstructURI(ServiceInstance instance, URI original) {
			throw new UnsupportedOperationException("Please, implement me.");
		}

		@Override
		public ServiceInstance choose(String serviceId) {
			return defaultServiceInstance();
		}

		@Override
		public <T> ServiceInstance choose(String serviceId, Request<T> request) {
			return defaultServiceInstance();
		}

		Map<String, LoadBalancerLifecycle> getLifecycleProcessors() {
			return lifecycleProcessors;
		}

	}

	protected static class TestLoadBalancerLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {

		final ConcurrentHashMap<String, Request<Object>> startLog = new ConcurrentHashMap<>();

		final ConcurrentHashMap<String, Request<Object>> startRequestLog = new ConcurrentHashMap<>();

		final ConcurrentHashMap<String, CompletionContext<Object, ServiceInstance, Object>> completeLog = new ConcurrentHashMap<>();

		@Override
		public boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
			return DefaultRequestContext.class.isAssignableFrom(requestContextClass)
					&& Object.class.isAssignableFrom(responseClass)
					&& ServiceInstance.class.isAssignableFrom(serverTypeClass);
		}

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

		ConcurrentHashMap<String, Request<Object>> getStartLog() {
			return startLog;
		}

		ConcurrentHashMap<String, CompletionContext<Object, ServiceInstance, Object>> getCompleteLog() {
			return completeLog;
		}

		ConcurrentHashMap<String, Request<Object>> getStartRequestLog() {
			return startRequestLog;
		}

		protected String getName() {
			return getClass().getSimpleName();
		}

	}

	protected static class AnotherLoadBalancerLifecycle extends TestLoadBalancerLifecycle {

		@Override
		protected String getName() {
			return getClass().getSimpleName();
		}

	}

}
