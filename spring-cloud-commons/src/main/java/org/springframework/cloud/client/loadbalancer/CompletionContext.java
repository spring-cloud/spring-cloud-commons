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

import org.springframework.core.style.ToStringCreator;

/**
 * Allows propagation of data related to load-balanced call completion status.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class CompletionContext<RES, T, C> {

	private final Status status;

	private final Throwable throwable;

	private final Response<T> loadBalancerResponse;

	private final RES clientResponse;

	private final Request<C> loadBalancerRequest;

	public CompletionContext(Status status, Request<C> loadBalancerRequest) {
		this(status, null, loadBalancerRequest, null, null);
	}

	public CompletionContext(Status status, Request<C> loadBalancerRequest, Response<T> response) {
		this(status, null, loadBalancerRequest, response, null);
	}

	public CompletionContext(Status status, Throwable throwable, Request<C> loadBalancerRequest,
			Response<T> loadBalancerResponse) {
		this(status, throwable, loadBalancerRequest, loadBalancerResponse, null);
	}

	public CompletionContext(Status status, Request<C> loadBalancerRequest, Response<T> loadBalancerResponse,
			RES clientResponse) {
		this(status, null, loadBalancerRequest, loadBalancerResponse, clientResponse);
	}

	public CompletionContext(Status status, Throwable throwable, Request<C> loadBalancerRequest,
			Response<T> loadBalancerResponse, RES clientResponse) {
		this.status = status;
		this.throwable = throwable;
		this.loadBalancerRequest = loadBalancerRequest;
		this.loadBalancerResponse = loadBalancerResponse;
		this.clientResponse = clientResponse;
	}

	public Status status() {
		return this.status;
	}

	public Throwable getThrowable() {
		return this.throwable;
	}

	public Response<T> getLoadBalancerResponse() {
		return loadBalancerResponse;
	}

	public RES getClientResponse() {
		return clientResponse;
	}

	public Request<C> getLoadBalancerRequest() {
		return loadBalancerRequest;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("status", this.status);
		to.append("throwable", this.throwable);
		to.append("loadBalancerResponse", loadBalancerResponse);
		to.append("clientResponse", clientResponse);
		return to.toString();
	}

	/**
	 * Request status state.
	 */
	public enum Status {

		/** Request was handled successfully. */
		SUCCESS,
		/** Request reached the server but failed due to timeout or internal error. */
		FAILED,
		/** Request did not go off box and should not be counted for statistics. */
		DISCARD,

	}

}
