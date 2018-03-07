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
    private LoadBalancedRetryFactory loadBalancedRetryFactory = new LoadBalancedRetryFactory(){};

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
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException());
        lbProperties.setEnabled(false);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRequestFactory, loadBalancedRetryFactory);
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
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRequestFactory,
                loadBalancedRetryFactory);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        interceptor.intercept(request, body, execution);
    }

    @Test
    public void interceptNeverRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRequestFactory, loadBalancedRetryFactory);
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
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRequestFactory, new MyLoadBalancedRetryFactory(policy));
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
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), nullable(LoadBalancerRequest.class))).
                thenReturn(clientHttpResponseNotFound).thenReturn(clientHttpResponseOk);
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRequestFactory, new MyLoadBalancedRetryFactory(policy));
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

        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), ArgumentMatchers.<LoadBalancerRequest<ClientHttpResponse>>any()))
                .thenReturn(clientHttpResponseNotFound);

        lbProperties.setEnabled(true);
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRequestFactory, new MyLoadBalancedRetryFactory(policy));
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
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRequestFactory,
                new MyLoadBalancedRetryFactory(policy, backOffPolicy));
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
        assertThat(backOffPolicy.getBackoffAttempts(), is(1));
    }

    @Test(expected = IOException.class)
    public void interceptFailedRetry() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://foo"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(false);
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("foo"))).thenReturn(serviceInstance);
        when(client.execute(eq("foo"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
		when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        lbProperties.setEnabled(true);
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties,
                lbRequestFactory, new MyLoadBalancedRetryFactory(policy));
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        interceptor.intercept(request, body, execution);
        verify(lbRequestFactory).createRequest(request, body, execution);
    }

    @Test
    public void retryListenerTest() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://listener"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("listener"))).thenReturn(serviceInstance);
        when(client.execute(eq("listener"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
        lbProperties.setEnabled(true);
        MyRetryListener retryListener = new MyRetryListener();
        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRequestFactory,
            new MyLoadBalancedRetryFactory(policy, backOffPolicy, new RetryListener[]{retryListener}));
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("listener"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
        assertThat(backOffPolicy.getBackoffAttempts(), is(1));
        assertThat(retryListener.getOnError(), is(1));
    }

    @Test(expected = TerminatedRetryException.class)
    public void retryListenerTestNoRetry() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://noRetry"));
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        lbProperties.setEnabled(true);
        RetryListener myRetryListener = new RetryListenerSupport(){
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                return false;
            }
        };
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRequestFactory,
            new MyLoadBalancedRetryFactory(policy, backOffPolicy, new RetryListener[]{myRetryListener}));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        interceptor.intercept(request, new byte[]{}, execution);
    }

    @Test
    public void retryWithDefaultConstructorTest() throws Throwable {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://default"));
        ClientHttpResponse clientHttpResponse = new MockClientHttpResponse(new byte[]{}, HttpStatus.OK);
        LoadBalancedRetryPolicy policy = mock(LoadBalancedRetryPolicy.class);
        when(policy.canRetryNextServer(any(LoadBalancedRetryContext.class))).thenReturn(true);
        MyBackOffPolicy backOffPolicy = new MyBackOffPolicy();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(client.choose(eq("default"))).thenReturn(serviceInstance);
        when(client.execute(eq("default"), eq(serviceInstance), any(LoadBalancerRequest.class))).thenThrow(new IOException()).thenReturn(clientHttpResponse);
        lbProperties.setEnabled(true);
        when(this.lbRequestFactory.createRequest(any(), any(), any())).thenReturn(mock(LoadBalancerRequest.class));
        RetryLoadBalancerInterceptor interceptor = new RetryLoadBalancerInterceptor(client, lbProperties, lbRequestFactory,
            new MyLoadBalancedRetryFactory(policy, backOffPolicy));
        byte[] body = new byte[]{};
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse rsp = interceptor.intercept(request, body, execution);
        verify(client, times(2)).execute(eq("default"), eq(serviceInstance), any(LoadBalancerRequest.class));
        assertThat(rsp, is(clientHttpResponse));
        verify(lbRequestFactory, times(2)).createRequest(request, body, execution);
        assertThat(backOffPolicy.getBackoffAttempts(), is(1));
    }

    class MyLoadBalancedRetryFactory implements LoadBalancedRetryFactory {
        private LoadBalancedRetryPolicy loadBalancedRetryPolicy;
        private BackOffPolicy backOffPolicy;
        private RetryListener[] retryListeners;

        public MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy) {
            this.loadBalancedRetryPolicy = loadBalancedRetryPolicy;
        }

        public MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy, BackOffPolicy backOffPolicy) {
        	this(loadBalancedRetryPolicy);
        	this.backOffPolicy = backOffPolicy;
		}

		public MyLoadBalancedRetryFactory(LoadBalancedRetryPolicy loadBalancedRetryPolicy, BackOffPolicy backOffPolicy,
										  RetryListener[] retryListeners) {
        	this(loadBalancedRetryPolicy, backOffPolicy);
        	this.retryListeners = retryListeners;
		}

        @Override
        public LoadBalancedRetryPolicy createRetryPolicy(String service, ServiceInstanceChooser serviceInstanceChooser) {
            return loadBalancedRetryPolicy;
        }

		@Override
		public BackOffPolicy createBackOffPolicy(String service) {
			if(backOffPolicy == null) {
				return new NoBackOffPolicy();
			} else {
				return backOffPolicy;
			}
		}

		@Override
		public RetryListener[] createRetryListeners(String service) {
			if(retryListeners == null) {
				return new RetryListener[0];
			} else {
				return retryListeners;
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
        public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
            backoffAttempts++;
        }

        int getBackoffAttempts() {
            return backoffAttempts;
        }
    }

    class MyRetryListener extends RetryListenerSupport {

        private int onError = 0;

		@Override
		public <T, E extends Throwable> void onError(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {
            onError++;
		}

        int getOnError() {
            return onError;
        }
	}
}