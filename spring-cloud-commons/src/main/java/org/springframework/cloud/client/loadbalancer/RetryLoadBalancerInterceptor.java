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

import java.io.IOException;
import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
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


	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer, RetryTemplate retryTemplate,
										LoadBalancerRetryProperties lbProperties,
										LoadBalancedRetryPolicyFactory lbRetryPolicyFactory,
										LoadBalancerRequestFactory requestFactory) {
		this.loadBalancer = loadBalancer;
		this.lbRetryPolicyFactory = lbRetryPolicyFactory;
		this.retryTemplate = retryTemplate;
		this.lbProperties = lbProperties;
		this.requestFactory = requestFactory;
	}
	
	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer, RetryTemplate retryTemplate,
										LoadBalancerRetryProperties lbProperties,
										LoadBalancedRetryPolicyFactory lbRetryPolicyFactory) {
		// for backwards compatibility
		this(loadBalancer, retryTemplate, lbProperties, lbRetryPolicyFactory, 
				new LoadBalancerRequestFactory(loadBalancer));
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
										final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
		final LoadBalancedRetryPolicy retryPolicy = lbRetryPolicyFactory.create(serviceName,
				loadBalancer);
		retryTemplate.setRetryPolicy(
				!lbProperties.isEnabled() || retryPolicy == null ? new NeverRetryPolicy()
						: new InterceptorRetryPolicy(request, retryPolicy, loadBalancer,
						serviceName));
		return retryTemplate
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
						if(retryPolicy != null && retryPolicy.retryableStatusCode(response.getRawStatusCode())) {
							throw new RetryableStatusCodeException(serviceName, response.getRawStatusCode());
						}
						return response;
					}
				});
	}
}
