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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Stores the data for a load-balanced call that is being retried.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class LoadBalancerRetryContext {

	private final ClientRequest request;

	private ClientResponse clientResponse;

	private Integer retriesSameServiceInstance = 0;

	private Integer retriesNextServiceInstance = 0;

	protected LoadBalancerRetryContext(ClientRequest request) {
		this.request = request;
	}

	/**
	 * Returns the {@link ClientRequest} that is being load-balanced.
	 * @return the request that is being load-balanced.
	 */
	protected ClientRequest getRequest() {
		return request;
	}

	/**
	 * Returns the {@link ClientResponse} returned for load-balanced request.
	 * @return the response for the load-balanced request.
	 */
	protected ClientResponse getClientResponse() {
		return clientResponse;
	}

	/**
	 * Sets the {@link ClientResponse} returned for load-balanced request.
	 * @param clientResponse the response for the load-balanced request.
	 */
	protected void setClientResponse(ClientResponse clientResponse) {
		this.clientResponse = clientResponse;
	}

	/**
	 * Returns the number of times a load-balanced request should be retried on the same
	 * {@link ServiceInstance}.
	 * @return the number of retries
	 */
	protected Integer getRetriesSameServiceInstance() {
		return retriesSameServiceInstance;
	}

	/**
	 * Increments the counter for the retries executed against the same
	 * {@link ServiceInstance}.
	 */
	protected void incrementRetriesSameServiceInstance() {
		retriesSameServiceInstance++;
	}

	/**
	 * Resets the counter for the retries executed against the same
	 * {@link ServiceInstance}.
	 */
	protected void resetRetriesSameServiceInstance() {
		retriesSameServiceInstance = 0;
	}

	/**
	 * Returns the number of times a load-balanced request should be retried on the next
	 * {@link ServiceInstance}.
	 * @return the number of retries
	 */
	protected Integer getRetriesNextServiceInstance() {
		return retriesNextServiceInstance;
	}

	/**
	 * Increments the counter for the retries executed against the same
	 * {@link ServiceInstance}.
	 */
	protected void incrementRetriesNextServiceInstance() {
		retriesNextServiceInstance++;
	}

	/**
	 * Returns the status code from the {@link ClientResponse} returned for load-balanced
	 * request.
	 * @return the status code from the response for the load-balanced request.
	 */
	protected Integer getResponseStatusCode() {
		return clientResponse.statusCode().value();
	}

	/**
	 * Returns the {@link HttpMethod} of the {@link ClientRequest} that is being
	 * load-balanced.
	 * @return the HTTP method of the request that is being load-balanced.
	 */
	protected HttpMethod getRequestMethod() {
		return request.method();
	}

}
