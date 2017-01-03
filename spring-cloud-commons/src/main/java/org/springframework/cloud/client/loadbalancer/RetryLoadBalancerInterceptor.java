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
 */
public class RetryLoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private LoadBalancedRetryPolicyFactory lbRetryPolicyFactory;
	private RetryTemplate retryTemplate;
	private LoadBalancerClient loadBalancer;
	private LoadBalancerRetryProperties lbProperties;


	public RetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer, RetryTemplate retryTemplate,
										LoadBalancerRetryProperties lbProperties,
										LoadBalancedRetryPolicyFactory lbRetryPolicyFactory) {
		this.loadBalancer = loadBalancer;
		this.lbRetryPolicyFactory = lbRetryPolicyFactory;
		this.retryTemplate = retryTemplate;
		this.lbProperties = lbProperties;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
										final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
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
						return RetryLoadBalancerInterceptor.this.loadBalancer.execute(
								serviceName, serviceInstance,
								new LoadBalancerRequest<ClientHttpResponse>() {

									@Override
									public ClientHttpResponse apply(
											final ServiceInstance instance)
											throws Exception {
										HttpRequest serviceRequest = new ServiceRequestWrapper(
												request, instance, loadBalancer);
										return execution.execute(serviceRequest, body);
									}

								});
					}
				});
	}
}
