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

import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Reactive load balancer.
 *
 * @param <T> type of the response
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public interface ReactiveLoadBalancer<T> {

	/**
	 * Default implementation of a request.
	 */
	Request<DefaultRequestContext> REQUEST = new DefaultRequest();

	/**
	 * Choose the next server based on the load balancing algorithm.
	 * @param request - incoming request
	 * @return publisher for the response, never null.
	 */
	@SuppressWarnings("rawtypes")
	Publisher<Response<T>> choose(Request request);

	default Publisher<Response<T>> choose() { // conflicting name
		return choose(REQUEST);
	}

	/**
	 * Executes a request using {@link ServiceInstance} provided in LoadBalancer
	 * {@link Response}.
	 * @param execution defining how a LoadBalancer {@link Response} should be mapped to
	 * the request execution result {@link Publisher}
	 * @param <R> execution result type
	 * @param <C> LoadBalancer {@link Request} context type
	 * @return a {@link Publisher} of execution result
	 */
	<R, C> Publisher<R> execute(RequestExecution<R, C, T> execution);

	/**
	 * Executes a request using {@link ServiceInstance} provided in LoadBalancer
	 * {@link Response} and calls
	 * {@link LoadBalancedCallExecution.Callback#onComplete(LoadBalancedCallExecution)} at
	 * the end.
	 * @param execution defines how a LoadBalancer {@link Response} should be mapped to
	 * the request execution result {@link Publisher}
	 * @param callback object on which to call the <code>onComplete()</code> method
	 * @param <R> execution result type
	 * @param <C> LoadBalancer {@link Request} context type
	 * @return a {@link Publisher} of execution result
	 */
	<R, C> Publisher<R> execute(RequestExecution<R, C, T> execution,
			LoadBalancedCallExecution.Callback<C, T> callback);

	/**
	 * Defines how a LoadBalancer {@link Response} should be mapped to the request
	 * execution result {@link Publisher}.
	 *
	 * @param <R> execution result type
	 * @param <C> LoadBalancer {@link Request} context type
	 * @param <TT> LoadBalancer {@link Response} type
	 */
	interface RequestExecution<R, C, TT> extends Function<Response<TT>, Publisher<R>> {

		default Request<C> createRequest() {
			return new DefaultRequest<>();
		}

		default CompletionContext createCompletionContext(CompletionContext.Status status,
				R clientResponse, Throwable throwable) {
			return new DefaultCompletionContext(status, clientResponse, throwable);
		}

		default LoadBalancedCallExecution<C, TT> createLoadBalancedCallExecution(
				Request<C> request, Response<TT> response,
				CompletionContext completionContext) {
			return new DefaultLoadBalancedCallExecution<>(request, response,
					completionContext);
		}

	}

	@FunctionalInterface
	interface Factory<T> {

		ReactiveLoadBalancer<T> getInstance(String serviceId);

	}

}
