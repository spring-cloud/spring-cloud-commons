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

package org.springframework.cloud.loadbalancer.core;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.loadbalancer.reactive.CompletionContext;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedCallExecution;
import org.springframework.cloud.client.loadbalancer.reactive.NoOpLoadBalancerResponseCallback;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;

/**
 * A Reactor based implementation of {@link ReactiveLoadBalancer}.
 *
 * @param <T> - type of the response
 * @author Spencer Gibb
 */
public interface ReactorLoadBalancer<T> extends ReactiveLoadBalancer<T> {

	/**
	 * Choose the next server based on the load balancing algorithm.
	 * @param request - an input request
	 * @return - mono of response
	 */
	@SuppressWarnings("rawtypes")
	Mono<Response<T>> choose(Request request);

	default Mono<Response<T>> choose() {
		return choose(REQUEST);
	}

	@Override
	default <R, C> Publisher<R> execute(RequestExecution<R, C, T> execution) {
		return execute(execution, new NoOpLoadBalancerResponseCallback<>());
	}

	@Override
	default <R, C> Publisher<R> execute(RequestExecution<R, C, T> execution,
			LoadBalancedCallExecution.Callback<C, T, R> callback) {
		Request<C> request = execution.createRequest();
		return choose(request).flatMap(response -> {
			if (!response.hasServer()) {
				callback.onComplete(execution.createLoadBalancedCallExecutionData(request,
						response, execution.createCompletionContext(
								CompletionContext.Status.DISCARD, null, null)));
				return Mono.from(execution.apply(response));
			}
			return Mono.from(execution.apply(response))
					.doOnSuccess(clientResponse -> callback.onComplete(execution
							.createLoadBalancedCallExecutionData(request, response,
									execution.createCompletionContext(
											CompletionContext.Status.SUCCESS,
											clientResponse, null))))
					.doOnError(throwable -> callback.onComplete(execution
							.createLoadBalancedCallExecutionData(request, response,
									execution.createCompletionContext(
											CompletionContext.Status.FAILED, null,
											throwable))));
		});
	}

}
