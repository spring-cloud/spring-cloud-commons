/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Default {@link LoadBalancerRequest} implementation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.1.2
 */
class BlockingLoadBalancerRequest implements HttpRequestLoadBalancerRequest<ClientHttpResponse> {

	private final LoadBalancerClient loadBalancer;

	private final List<LoadBalancerRequestTransformer> transformers;

	private final ClientHttpRequestData clientHttpRequestData;

	BlockingLoadBalancerRequest(LoadBalancerClient loadBalancer, List<LoadBalancerRequestTransformer> transformers,
			ClientHttpRequestData clientHttpRequestData) {
		this.loadBalancer = loadBalancer;
		this.transformers = transformers;
		this.clientHttpRequestData = clientHttpRequestData;
	}

	@Override
	public ClientHttpResponse apply(ServiceInstance instance) throws Exception {
		HttpRequest serviceRequest = new ServiceRequestWrapper(clientHttpRequestData.request, instance, loadBalancer);
		if (this.transformers != null) {
			for (LoadBalancerRequestTransformer transformer : this.transformers) {
				serviceRequest = transformer.transformRequest(serviceRequest, instance);
			}
		}
		return clientHttpRequestData.execution.execute(serviceRequest, clientHttpRequestData.body);
	}

	@Override
	public HttpRequest getHttpRequest() {
		return clientHttpRequestData.request;
	}

	static class ClientHttpRequestData {

		private final HttpRequest request;

		private final byte[] body;

		private final ClientHttpRequestExecution execution;

		ClientHttpRequestData(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
			this.request = request;
			this.body = body;
			this.execution = execution;
		}

	}

}
