/*
 * Copyright 2016-2017 the original author or authors.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * @author Ryan Baxter
 * @author Will Tran
 */
public class RetryLoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private LoadBalancedRetryPolicyFactory lbRetryPolicyFactory;
	private RetryTemplate retryTemplate;
	private LoadBalancerClient loadBalancer;
	private LoadBalancerRetryProperties lbProperties;
	private LoadBalancerRequestFactory requestFactory;
	private LoadBalancedBackOffPolicyFactory backOffPolicyFactory;


	@Deprecated
	//TODO remove in 2.0.x
	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer,
										LoadBalancerRetryProperties lbProperties,
										LoadBalancedRetryPolicyFactory lbRetryPolicyFactory,
										LoadBalancerRequestFactory requestFactory) {
		this.loadBalancer = loadBalancer;
		this.lbRetryPolicyFactory = lbRetryPolicyFactory;
		this.lbProperties = lbProperties;
		this.requestFactory = requestFactory;
		this.backOffPolicyFactory = new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
	}

	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer,
										LoadBalancerRetryProperties lbProperties,
										LoadBalancedRetryPolicyFactory lbRetryPolicyFactory,
										LoadBalancerRequestFactory requestFactory,
										LoadBalancedBackOffPolicyFactory backOffPolicyFactory) {
		this.loadBalancer = loadBalancer;
		this.lbRetryPolicyFactory = lbRetryPolicyFactory;
		this.lbProperties = lbProperties;
		this.requestFactory = requestFactory;
		this.backOffPolicyFactory = backOffPolicyFactory;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
										final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
		final LoadBalancedRetryPolicy retryPolicy = lbRetryPolicyFactory.create(serviceName,
				loadBalancer);
		RetryTemplate template = this.retryTemplate == null ? new RetryTemplate() : this.retryTemplate;
		BackOffPolicy backOffPolicy = backOffPolicyFactory.createBackOffPolicy(serviceName);
		template.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		template.setThrowLastExceptionOnExhausted(true);
		template.setRetryPolicy(
				!lbProperties.isEnabled() || retryPolicy == null ? new NeverRetryPolicy()
						: new InterceptorRetryPolicy(request, retryPolicy, loadBalancer,
						serviceName));
		return template
				.execute(new RetryCallback<ClientHttpResponse, IOException>() {
                    @Override
                    public ClientHttpResponse doWithRetry(RetryContext context)
                            throws IOException {
                        ServiceInstance serviceInstance = null;
                        if (context instanceof LoadBalancedRetryContext) {
                            LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
                            serviceInstance = lbContext.getServiceInstance();
                        }
                        if (serviceInstance == null) {
                            serviceInstance = loadBalancer.choose(serviceName);
                        }
                        ClientHttpResponse response = RetryLoadBalancerInterceptor.this.loadBalancer.execute(
                                serviceName, serviceInstance,
                                requestFactory.createRequest(request, body, execution));
                        int statusCode = response.getRawStatusCode();
                        if (retryPolicy != null && retryPolicy.retryableStatusCode(statusCode)) {
                            ClientHttpResponseWrapper wrapper = new ClientHttpResponseWrapper(response);
                            wrapper.init();
                            throw new RetryableStatusCodeException(serviceName, statusCode, wrapper, null);
                        }
                        return response;
                    }
                }, new RecoveryCallback<ClientHttpResponse>() {
                    @Override
                    public ClientHttpResponse recover(RetryContext retryContext) throws Exception {
                        Throwable lastThrowable = retryContext.getLastThrowable();
                        if (lastThrowable != null && lastThrowable instanceof RetryableStatusCodeException) {
                            RetryableStatusCodeException ex = (RetryableStatusCodeException) lastThrowable;
                            return (ClientHttpResponse) ex.getResponse();
                        }
                        throw new RetryException("Could not recover", lastThrowable);
                    }
                });
	}

    public static class ClientHttpResponseWrapper implements ClientHttpResponse {

	    private ClientHttpResponse response;
	    private InputStream content;

        public ClientHttpResponseWrapper(ClientHttpResponse response) {
            this.response = response;
        }

        public void init() throws IOException {
            InputStream body = response.getBody();
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length = 0;
            while ((length = body.read(buffer)) != -1) {
                temp.write(buffer, 0, length);
            }
            content = new ByteArrayInputStream(temp.toByteArray());
            response.close();
        }

        @Override
        public HttpStatus getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return response.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public InputStream getBody() throws IOException {
            return content;
        }

        @Override
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }
    }
}
