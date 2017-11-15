package org.springframework.cloud.client.loadbalancer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(null);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException());
        lbProperties.setEnabled(false);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }

    @Test(expected = IllegalStateException.class)
    public void interceptInvalidHost() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo_underscore"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, client,"foo");
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo_underscore"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo_underscore"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo_underscore"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
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
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(null);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
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
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
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
        InputStream notFoundStream = mock(InputStream.class);
        when(notFoundStream.read(any(byte[].class))).thenReturn(-1);
        ClientHttpResponse clientHttpResponseNotFound = new MockClientHttpResponse(notFoundStream, HttpStatus.NOT_FOUND);
        ClientHttpResponse clientHttpResponseOk = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.retryableStatusCode(eq(HttpStatus.NOT_FOUND.value()))).thenReturn(true);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        InterceptorRetryPolicy interceptorRetryPolicy = new InterceptorRetryPolicy(request, policy, client,"foo");
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).
                thenReturn(clientHttpResponseNotFound).thenReturn(clientHttpResponseOk);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class));
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
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).
                thenReturn(clientHttpResponseNotFound);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(1)).execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class));
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
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
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
        when(policy.canRetrySameServer(any(LoadBalancedRetryContext.class))).thenReturn(false);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(false);
        LoadBalancedRetryPolicyFactory lbRetryPolicyFactory = mock(LoadBalancedRetryPolicyFactory.class);
        when(lbRetryPolicyFactory.create(eq("foo"), any(ServiceInstanceChooser.class))).thenReturn(policy);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRetryPolicyFactory, lbRequestFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }
}