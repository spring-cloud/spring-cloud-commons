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

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author Rob Worsnop
 * @author Olga Maciaszek-Sharma
 */
public class AsyncLoadBalancerInterceptor implements AsyncClientHttpRequestInterceptor {

	private LoadBalancerClient loadBalancer;

	public AsyncLoadBalancerInterceptor(LoadBalancerClient loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	@Override
	public ListenableFuture<ClientHttpResponse> intercept(final HttpRequest request, final byte[] body,
			final AsyncClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String serviceName = originalUri.getHost();
		return loadBalancer.execute(serviceName, new AsyncLoadBalancerRequest(loadBalancer,
				new AsyncLoadBalancerRequest.ClientHttpRequestData(request, body, execution)));
	}

}
