package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerRetryContext {
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