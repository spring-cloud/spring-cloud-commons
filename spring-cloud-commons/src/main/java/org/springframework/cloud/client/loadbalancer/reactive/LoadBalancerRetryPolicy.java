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

package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.http.HttpMethod;

/**
 * Pluggable policy used to establish whether a given load-balanced call should be
 * retried.
 *
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @since 3.0.0
 */
public interface LoadBalancerRetryPolicy {

	/**
	 * Return <code>true</code> to retry on the same service instance.
	 * @param context the context for the retry operation
	 * @return true to retry on the same service instance
	 * @deprecated Deprecated in favor of
	 * {@link #canRetrySameServiceInstance(String, LoadBalancerRetryContext)}
	 */
	@Deprecated
	boolean canRetrySameServiceInstance(LoadBalancerRetryContext context);

	/**
	 * Return <code>true</code> to retry on the same service instance.
	 * @param serviceId the serviceId for the retry operation
	 * @param context the context for the retry operation
	 * @return true to retry on the same service instance
	 */
	default boolean canRetrySameServiceInstance(String serviceId, LoadBalancerRetryContext context) {
		return canRetrySameServiceInstance(context);
	}

	/**
	 * Return <code>true</code> to retry on the next service instance.
	 * @param context the context for the retry operation
	 * @return true to retry on the same service instance
	 * @deprecated Deprecated in favor of
	 * {@link #canRetryNextServiceInstance(String, LoadBalancerRetryContext)}
	 */
	@Deprecated
	boolean canRetryNextServiceInstance(LoadBalancerRetryContext context);

	/**
	 * Return <code>true</code> to retry on the next service instance.
	 * @param serviceId the serviceId for the retry operation
	 * @param context the context for the retry operation
	 * @return true to retry on the same service instance
	 */
	default boolean canRetryNextServiceInstance(String serviceId, LoadBalancerRetryContext context) {
		return canRetryNextServiceInstance(context);
	}

	/**
	 * Return <code>true</code> to retry on the provided HTTP status code.
	 * @param statusCode the HTTP status code
	 * @return true to retry on the provided HTTP status code
	 * @deprecated Deprecated in favor of {@link #retryableStatusCode(String, int)}
	 */
	@Deprecated
	boolean retryableStatusCode(int statusCode);

	/**
	 * Return <code>true</code> to retry on the provided HTTP status code.
	 * @param serviceId the serviceId for the retry operation
	 * @param statusCode the HTTP status code
	 * @return true to retry on the provided HTTP status code
	 */
	default boolean retryableStatusCode(String serviceId, int statusCode) {
		return retryableStatusCode(statusCode);
	}

	/**
	 * Return <code>true</code> to retry on the provided HTTP method.
	 * @param method the HTTP request method
	 * @return true to retry on the provided HTTP method
	 * @deprecated Deprecated in favor of {@link #canRetryOnMethod(String, HttpMethod)}
	 */
	@Deprecated
	boolean canRetryOnMethod(HttpMethod method);

	/**
	 * Return <code>true</code> to retry on the provided HTTP method.
	 * @param serviceId the serviceId for the retry operation
	 * @param method the HTTP request method
	 * @return true to retry on the provided HTTP method
	 */
	default boolean canRetryOnMethod(String serviceId, HttpMethod method) {
		return canRetryOnMethod(method);
	}

}
