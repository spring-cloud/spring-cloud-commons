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

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * @author Ryan Baxter
 * @author Will Tran
 * @author Gang Li
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RetryLoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private static final Log LOG = LogFactory.getLog(RetryLoadBalancerInterceptor.class);

	private final LoadBalancerClient loadBalancer;

	private final LoadBalancerRequestFactory requestFactory;

	private final LoadBalancedRetryFactory lbRetryFactory;

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

	/**
	 * @deprecated in favour of
	 * {@link RetryLoadBalancerInterceptor#RetryLoadBalancerInterceptor(LoadBalancerClient, LoadBalancerRequestFactory, LoadBalancedRetryFactory, ReactiveLoadBalancer.Factory)}
	 */
	@Deprecated
	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerProperties properties,
			LoadBalancerRequestFactory requestFactory, LoadBalancedRetryFactory lbRetryFactory,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
		this.lbRetryFactory = lbRetryFactory;
		this.loadBalancerFactory = loadBalancerFactory;
	}

	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerRequestFactory requestFactory,
			LoadBalancedRetryFactory lbRetryFactory,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
		this.lbRetryFactory = lbRetryFactory;
		this.loadBalancerFactory = loadBalancerFactory;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
			final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
		final LoadBalancedRetryPolicy retryPolicy = lbRetryFactory.createRetryPolicy(serviceName, loadBalancer);
		RetryTemplate template = createRetryTemplate(serviceName, request, retryPolicy);
		return template.execute(context -> {
			ServiceInstance serviceInstance = null;
			if (context instanceof LoadBalancedRetryContext) {
				LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
				serviceInstance = lbContext.getServiceInstance();
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrieved service instance from LoadBalancedRetryContext: %s",
							serviceInstance));
				}
			}
			Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
					.getSupportedLifecycleProcessors(
							loadBalancerFactory.getInstances(serviceName, LoadBalancerLifecycle.class),
							RetryableRequestContext.class, ResponseData.class, ServiceInstance.class);
			String hint = getHint(serviceName);
			if (serviceInstance == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Service instance retrieved from LoadBalancedRetryContext: was null. "
							+ "Reattempting service instance selection");
				}
				ServiceInstance previousServiceInstance = null;
				if (context instanceof LoadBalancedRetryContext) {
					LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
					previousServiceInstance = lbContext.getPreviousServiceInstance();
				}
				DefaultRequest<RetryableRequestContext> lbRequest = new DefaultRequest<>(
						new RetryableRequestContext(previousServiceInstance, new RequestData(request), hint));
				supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
				serviceInstance = loadBalancer.choose(serviceName, lbRequest);
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Selected service instance: %s", serviceInstance));
				}
				if (context instanceof LoadBalancedRetryContext) {
					LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
					lbContext.setServiceInstance(serviceInstance);
				}
				Response<ServiceInstance> lbResponse = new DefaultResponse(serviceInstance);
				if (serviceInstance == null) {
					supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
							.onComplete(new CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>(
									CompletionContext.Status.DISCARD,
									new DefaultRequest<>(
											new RetryableRequestContext(null, new RequestData(request), hint)),
									lbResponse)));
				}
			}
			LoadBalancerRequestAdapter<ClientHttpResponse, RetryableRequestContext> lbRequest = new LoadBalancerRequestAdapter<>(
					requestFactory.createRequest(request, body, execution),
					new RetryableRequestContext(null, new RequestData(request), hint));
			ServiceInstance finalServiceInstance = serviceInstance;
			ClientHttpResponse response = RetryLoadBalancerInterceptor.this.loadBalancer.execute(serviceName,
					finalServiceInstance, lbRequest);
			int statusCode = response.getRawStatusCode();
			if (retryPolicy != null && retryPolicy.retryableStatusCode(statusCode)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrying on status code: %d", statusCode));
				}
				byte[] bodyCopy = StreamUtils.copyToByteArray(response.getBody());
				response.close();
				throw new ClientHttpResponseStatusCodeException(serviceName, response, bodyCopy);
			}
			return response;
		}, new LoadBalancedRecoveryCallback<ClientHttpResponse, ClientHttpResponse>() {
			// This is a special case, where both parameters to
			// LoadBalancedRecoveryCallback are
			// the same. In most cases they would be different.
			@Override
			protected ClientHttpResponse createResponse(ClientHttpResponse response, URI uri) {
				return response;
			}
		});
	}

	private RetryTemplate createRetryTemplate(String serviceName, HttpRequest request,
			LoadBalancedRetryPolicy retryPolicy) {
		RetryTemplate template = new RetryTemplate();
		BackOffPolicy backOffPolicy = lbRetryFactory.createBackOffPolicy(serviceName);
		template.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		template.setThrowLastExceptionOnExhausted(true);
		RetryListener[] retryListeners = lbRetryFactory.createRetryListeners(serviceName);
		if (retryListeners != null && retryListeners.length != 0) {
			template.setListeners(retryListeners);
		}
		template.setRetryPolicy(
				!loadBalancerFactory.getProperties(serviceName).getRetry().isEnabled() || retryPolicy == null
						? new NeverRetryPolicy()
						: new InterceptorRetryPolicy(request, retryPolicy, loadBalancer, serviceName));
		return template;
	}

	private String getHint(String serviceId) {
		Map<String, String> hint = loadBalancerFactory.getProperties(serviceId).getHint();
		String defaultHint = hint.getOrDefault("default", "default");
		String hintPropertyValue = hint.get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}

}
