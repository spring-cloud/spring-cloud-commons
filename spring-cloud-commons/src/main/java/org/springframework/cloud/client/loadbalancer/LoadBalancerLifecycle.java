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

/**
 * Allows to define actions that should be carried out before and after load-balancing.
 *
 * @author Olga Maciaszek-Sharma
 */
public interface LoadBalancerLifecycle<RC, RES, T> {

	/**
	 * Allows to assess whether the lifecycle bean's callbacks should be executed. Some
	 * examples of possible implementations could comprise of verifying whether the
	 * classes passed in parameters are exactly or extend the classes that this lifecycle
	 * bean should process.
	 * @param requestContextClass The class of the {@link Request} <code>context</code>
	 * @param responseClass The class of the {@link CompletionContext}
	 * <code>clientResponse</code>
	 * @param serverTypeClass The type of Server that the LoadBalancer retrieves
	 * @return <code>true</code> if the lifecycle should be used to process given classes
	 */
	default boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
		return true;
	}

	/**
	 * A callback method executed before load-balancing.
	 * @param request the {@link Request} that will be used by the LoadBalancer to select
	 * a service instance
	 */
	void onStart(Request<RC> request);

	/**
	 * A callback method executed after a service instance has been selected, before
	 * executing the actual load-balanced request.
	 * @param request the {@link Request} that has been used by the LoadBalancer to select
	 * a service instance
	 * @param lbResponse the {@link Response} returned by the LoadBalancer
	 */
	void onStartRequest(Request<RC> request, Response<T> lbResponse);

	/**
	 * A callback method executed after load-balancing.
	 * @param completionContext the {@link CompletionContext} containing data relevant to
	 * the load-balancing and the response returned from the selected service instance
	 */
	void onComplete(CompletionContext<RES, T, RC> completionContext);

}
