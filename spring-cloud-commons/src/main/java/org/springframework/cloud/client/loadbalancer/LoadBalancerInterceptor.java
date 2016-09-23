/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.net.URI;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Ryan Baxter
 */
public class LoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private LoadBalancerClient loadBalancer;
	private RetryTemplate retryTemplate;
	private LoadBalancerRetryProperties lbProperties;
	private LoadBalancedRetryPolicyFactory lbRetryPolicyFactory;

	public LoadBalancerInterceptor(LoadBalancerClient loadBalancer, RetryTemplate retryTemplate, LoadBalancerRetryProperties lbProperties,
								   LoadBalancedRetryPolicyFactory lbRetryPolicyFactory) {
		this.loadBalancer = loadBalancer;
		this.retryTemplate = retryTemplate;
		this.lbProperties = lbProperties;
		this.lbRetryPolicyFactory = lbRetryPolicyFactory;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
			final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		LoadBalancedRetryPolicy retryPolicy = lbRetryPolicyFactory.create(serviceName,
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
						return LoadBalancerInterceptor.this.loadBalancer.execute(
								serviceName, serviceInstance,
								new LoadBalancerRequest<ClientHttpResponse>() {

									@Override
									public ClientHttpResponse apply(
											final ServiceInstance instance)
											throws Exception {
										HttpRequest serviceRequest = new ServiceRequestWrapper(
												request, instance);
										return execution.execute(serviceRequest, body);
									}

								});
					}
				});
	}

	private class ServiceRequestWrapper extends HttpRequestWrapper {

		private final ServiceInstance instance;

		public ServiceRequestWrapper(HttpRequest request, ServiceInstance instance) {
			super(request);
			this.instance = instance;
		}

		@Override
		public URI getURI() {
			URI uri = LoadBalancerInterceptor.this.loadBalancer.reconstructURI(
					this.instance, getRequest().getURI());
			return uri;
		}

	}

}
