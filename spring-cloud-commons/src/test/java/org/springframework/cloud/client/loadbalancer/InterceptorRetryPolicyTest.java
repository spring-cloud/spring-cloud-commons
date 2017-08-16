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

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
    private LoadBalancer loadBalancer;
    private String serviceName;

    @Before
    public void setup() {
        request = mock(HttpRequest.class);
        policy = mock(LoadBalancedRetryPolicy.class);
        loadBalancer = mock(LoadBalancer.class);
        serviceName = "foo";
    }

    @After
    public void teardown() {
        request = null;
        policy = null;
        loadBalancer = null;
        serviceName = null;
    }

    @Test
    public void canRetryBeforeExecution() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
        when(context.getRetryCount()).thenReturn(0);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(loadBalancer.choose(eq(serviceName))).thenReturn(serviceInstance);
        assertThat(interceptorRetryPolicy.canRetry(context), is(true));
        verify(context, times(1)).setServiceInstance(eq(serviceInstance));

    }

    @Test
    public void canRetryNextServer() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
        when(context.getRetryCount()).thenReturn(1);
        when(policy.canRetryNextServer(eq(context))).thenReturn(true);
        assertThat(interceptorRetryPolicy.canRetry(context), is(true));
    }

    @Test
    public void cannotRetry() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
        when(context.getRetryCount()).thenReturn(1);
        assertThat(interceptorRetryPolicy.canRetry(context), is(false));
    }

    @Test
    public void open() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        RetryContext context = interceptorRetryPolicy.open(null);
        assertThat(context, IsInstanceOf.instanceOf(LoadBalancedRetryContext.class));
    }

    @Test
    public void close() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
        interceptorRetryPolicy.close(context);
        verify(policy, times(1)).close(eq(context));
    }

    @Test
    public void registerThrowable() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);
        Throwable thrown = new Exception();
        interceptorRetryPolicy.registerThrowable(context, thrown);
        verify(context, times(1)).registerThrowable(eq(thrown));
        verify(policy, times(1)).registerThrowable(eq(context), eq(thrown));
    }

    @Test
    public void equals() throws Exception {
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName);
        assertThat(interceptorRetryPolicy.equals(null), is(false));
        assertThat(interceptorRetryPolicy.equals(new Object()), is(false));
        assertThat(interceptorRetryPolicy.equals(interceptorRetryPolicy), is(true));
        assertThat(interceptorRetryPolicy.equals(new InterceptorRetryPolicy(request, policy, loadBalancer, serviceName)), is(true));
    }

}