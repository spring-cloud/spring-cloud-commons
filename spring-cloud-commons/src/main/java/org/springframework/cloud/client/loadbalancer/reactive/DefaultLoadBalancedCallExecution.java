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
 * A default implementation of {@link LoadBalancedCallExecution}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class DefaultLoadBalancedCallExecution<C, T>
		implements LoadBalancedCallExecution<C, T> {

	private final Request<C> loadBalancerRequest;

	private final Response<T> loadBalancerResponse;

	private final CompletionContext completionContext;

	public DefaultLoadBalancedCallExecution(Request<C> loadBalancerRequest,
			Response<T> loadBalancerResponse, CompletionContext completionContext) {
		this.loadBalancerRequest = loadBalancerRequest;
		this.loadBalancerResponse = loadBalancerResponse;
		this.completionContext = completionContext;
	}

	@Override
	public Request<C> getLoadBalancerRequest() {
		return loadBalancerRequest;
	}

	@Override
	public Response<T> getLoadBalancerResponse() {
		return loadBalancerResponse;
	}

	@Override
	public CompletionContext getCompletionContext() {
		return completionContext;
	}

}
