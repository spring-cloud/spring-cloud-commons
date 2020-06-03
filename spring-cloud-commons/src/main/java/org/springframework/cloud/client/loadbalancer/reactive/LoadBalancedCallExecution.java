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

/**
 * Returns the data related to a load-balanced call execution.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public interface LoadBalancedCallExecution<C, T> {

	/**
	 * Returns the request that was passed to the LoadBalancer .
	 * @return a {@link Request} object
	 */
	Request<C> getLoadBalancerRequest();

	/**
	 * Returns the response returned by the LoadBalancer.
	 * @return a {@link Response} object
	 */
	Response<T> getLoadBalancerResponse();

	/**
	 * Returns the completion context associated with the load-balanced call execution.
	 * @return a {@link CompletionContext} object
	 */
	CompletionContext getCompletionContext();

	/**
	 * Allows to define actions that should happen upon the completion of a load-balanced
	 * call.
	 *
	 * @param <C> LoadBalancer {@link Request} context type
	 * @param <T> type of the LoadBalancer execution result
	 */
	interface Callback<C, T> {

		/**
		 * Notification that the request completed.
		 * @param execution the {@link LoadBalancedCallExecution} object containing the
		 * data of the load-balanced call that has completed
		 */
		void onComplete(LoadBalancedCallExecution<C, T> execution);

	}

}
