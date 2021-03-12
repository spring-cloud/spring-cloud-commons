/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Stores the data for a load-balanced call that is being retried.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
class LoadBalancerRetryContext {

	private final ClientRequest request;

	private ClientResponse clientResponse;

	private Integer retriesSameServiceInstance = 0;

	private Integer retriesNextServiceInstance = 0;

	LoadBalancerRetryContext(ClientRequest request) {
		this.request = request;
	}

	ClientRequest getRequest() {
		return request;
	}

	ClientResponse getClientResponse() {
		return clientResponse;
	}

	void setClientResponse(ClientResponse clientResponse) {
		this.clientResponse = clientResponse;
	}

	Integer getRetriesSameServiceInstance() {
		return retriesSameServiceInstance;
	}

	void incrementRetriesSameServiceInstance() {
		retriesSameServiceInstance++;
	}

	void resetRetriesSameServiceInstance() {
		retriesSameServiceInstance = 0;
	}

	Integer getRetriesNextServiceInstance() {
		return retriesNextServiceInstance;
	}

	void incrementRetriesNextServiceInstance() {
		retriesNextServiceInstance++;
	}

	Integer getResponseStatusCode() {
		return clientResponse.statusCode().value();
	}

	HttpMethod getRequestMethod() {
		return request.method();
	}

}
