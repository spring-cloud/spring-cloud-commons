package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface LoadBalancerRetryPolicy {

	/**
	 * Return true to retry the failed request on the same server. This method may be
	 * called more than once when executing a single operation.
	 * @param context The context for the retry operation.
	 * @return True to retry the failed request on the same server; false otherwise.
	 */
	boolean canRetrySameServiceInstance(LoadBalancerRetryContext context);

	/**
	 * Return true to retry the failed request on the next server from the load balancer.
	 * This method may be called more than once when executing a single operation.
	 * @param context The context for the retry operation.
	 * @return True to retry the failed request on the next server from the load balancer;
	 * false otherwise.
	 */
	boolean canRetryNextServiceInstance(LoadBalancerRetryContext context);

	/**
	 * If an exception is not thrown when making a request, this method will be called to
	 * see if the client would like to retry the request based on the status code
	 * returned. For example, in Cloud Foundry, the router will return a <code>404</code>
	 * when an app is not available. Since HTTP clients do not throw an exception when a
	 * <code>404</code> is returned, <code>retryableStatusCode</code> allows clients to
	 * force a retry.
	 * @param statusCode The HTTP status code.
	 * @return True if a retry should be attempted; false to just return the response.
	 */
	boolean retryableStatusCode(int statusCode);

	boolean canRetryOnMethod(HttpMethod method);
}
