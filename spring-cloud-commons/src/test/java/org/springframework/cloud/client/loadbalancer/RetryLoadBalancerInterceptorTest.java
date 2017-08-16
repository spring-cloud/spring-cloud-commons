/*
 * Copyright 2013-2017 the original author or authors.
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
import org.springframework.retry.RetryException;
import org.springframework.retry.RetryListener;
import org.springframework.retry.TerminatedRetryException;

import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
    private LoadBalancedBackOffPolicyFactory backOffPolicyFactory = new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
    private LoadBalancedRetryListenerFactory retryListenerFactory = new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();

    @Before
    public void setUp() throws Exception {
        client = mock(LoadBalancerClient.class);
        lbProperties = new LoadBalancerRetryProperties();
        lbRequestFactory = mock(LoadBalancerRequestFactory.class);

    }

    @After
    public void tearDown() throws Exception {
        client = null;
        lbProperties = null;
    }

    @Test(expected = RetryException.class)
    public void interceptDisableRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(null);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException());
        lbProperties.setEnabled(false);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory,
                lbRequestFactory, backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));

        interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }

    @Test(expected = IllegalStateException.class)
    public void interceptInvalidHost() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo_underscore"));
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory,
                backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
    }

    @Test
    public void interceptNeverRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(null);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory,
                lbRequestFactory, backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }

    @Test
    public void interceptSuccess() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, client,"foo");
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory,
                lbRequestFactory, backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory).createRequest(request, body, execution);
    }

    @Test
    public void interceptRetryOnStatusCode() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        InputStream notFoundStream = mock(InputStream.class);
        when(notFoundStream.read(any(byte[].class))).thenReturn(-1);
        ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(notFoundStream, HttpStatus.NOT_FOUND);
        ClientHttpResponse clientHttpResponseOk = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value()))).thenReturn(true);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, client,"foo");
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), nullable(LoadBalancerRequest.class))).
                thenReturn(clientHttpResponseNotFound).thenReturn(clientHttpResponseOk);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory,
                lbRequestFactory, backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), nullable(LoadBalancerRequest.class));
        verify(notFoundStream, times(1)).close();
        assertThat(rsp, is(clientHttpResponseOk));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
    }

    @Test
    public void interceptRetryFailOnStatusCode() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));

        InputStream notFoundStream = new ByteArrayInputStream("foo".getBytes());
        ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(notFoundStream, HttpStatus.NOT_FOUND);

        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value()))).thenReturn(true);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(false);

        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(policy);

        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any()))
                .thenReturn(clientHttpResponseNotFound);

        lbProperties.setEnabled(true);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRetryPolicyFactory, lbRequestFactory, backOffPolicyFactory);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);

        verify(client, times(1)).execute(eq("foo"), eq(serviceInstance),
                ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any());
        verify(lbRequestFactory, times(1)).createRequest(request, body, execution);
        verify(policy, times(2)).canRetryNextServer(any(LoadBalancedRetryContext.class));

        //call twice in a retry attempt
        byte[] content = new byte[1024];
        int length = rsp.getBody().read(content);
        assertThat(length, is("foo".getBytes().length));
        assertThat(new String(content, 0, length), is("foo"));
    }

    @Test
    public void interceptRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(policy);
        LoadBalancedBackOffPolicyFactory backOffPolicyFactory = mock(LoadBalancedBackOffPolicyFactory.class);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        when(backOffPolicyFactory.createBackOffPolicy(eq("foo"))).thenReturn(backOffPolicy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory,
                backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
        assertThat(backOffPolicy.getBackoffAttempts(), is(1));
    }

    @Test(expected = RetryException.class)
    public void interceptFailedRetry() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(false);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory,
                lbRequestFactory, backOffPolicyFactory, retryListenerFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }

    @Test
    public void retryListenerTest() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://listener"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("listener"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        LoadBalancedBackOffPolicyFactory backOffPolicyFactory = mock(LoadBalancedBackOffPolicyFactory.class);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        when(backOffPolicyFactory.createBackOffPolicy(eq("listener"))).thenReturn(backOffPolicy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("listener"))).thenReturn(serviceInstance);
        when(client.execute(eq("listener"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
        lbProperties.setEnabled(true);
        MyRetryListeners retryListeners = new MyRetryListeners();
        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory,
            backOffPolicyFactory, retryListeners);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("listener"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
        assertThat(backOffPolicy.getBackoffAttempts(), is(1));
        assertThat(retryListeners.getOnError(), is(1));
    }

    @Test(expected = TerminatedRetryException.class)
    public void retryListenerTestNoRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://noRetry"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("noRetry"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        LoadBalancedBackOffPolicyFactory backOffPolicyFactory = mock(LoadBalancedBackOffPolicyFactory.class);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        when(backOffPolicyFactory.createBackOffPolicy(eq("noRetry"))).thenReturn(backOffPolicy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        lbProperties.setEnabled(true);
        MyRetryListenersNotRetry retryListeners = new MyRetryListenersNotRetry();
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory,
            backOffPolicyFactory, retryListeners);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
    }

    @Test
    public void retryWithDefaultConstructorTest() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://default"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("default"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        LoadBalancedBackOffPolicyFactory backOffPolicyFactory = mock(LoadBalancedBackOffPolicyFactory.class);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        when(backOffPolicyFactory.createBackOffPolicy(eq("default"))).thenReturn(backOffPolicy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("default"))).thenReturn(serviceInstance);
        when(client.execute(eq("default"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
        lbProperties.setEnabled(true);
        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory,
            backOffPolicyFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("default"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
        assertThat(backOffPolicy.getBackoffAttempts(), is(1));
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
        public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
            backoffAttempts++;
        }

        public int getBackoffAttempts() {
            return backoffAttempts;
        }
    }

    class MyRetryListeners implements LoadBalancedRetryListenerFactory {

        private int onError = 0;

        @Override
        public RetryListener[] createRetryListeners(String service) {
            return new RetryListener[] {new RetryListener() {
                @Override
                public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                    return true;
                }

                @Override
                public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

                }

                @Override
                public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                    onError++;
                }
            }};
        }

        public int getOnError() {
            return onError;
        }
    }

    class MyRetryListenersNotRetry implements LoadBalancedRetryListenerFactory {

        @Override
        public RetryListener[] createRetryListeners(String service) {
            return new RetryListener[] {new RetryListener() {
                @Override
                public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                    return false;
                }

                @Override
                public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

                }

                @Override
                public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                }
            }};
        }

    }
}