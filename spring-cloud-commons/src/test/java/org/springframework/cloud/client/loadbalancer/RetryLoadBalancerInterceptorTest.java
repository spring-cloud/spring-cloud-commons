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

import java.io.IOException;
import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

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
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryLoadBalancerInterceptorTest {

    private LoadBalancerClient client;
    private LoadBalancerRetryProperties lbProperties;
    private LoadBalancerRequestFactory lbRequestFactory;

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

    @Test(expected = IOException.class)
    public void interceptDisableRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(null);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException());
        lbProperties.setEnabled(false);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
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
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
    }

    @Test
    public void interceptNeverRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(LoadBalancer.class))).thenReturn(null);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
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
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
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
        ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(new byte[]{}, HttpStatus.NOT_FOUND);
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
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), nullable(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponseOk));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
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
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
    }

    @Test(expected = IOException.class)
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
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }
}