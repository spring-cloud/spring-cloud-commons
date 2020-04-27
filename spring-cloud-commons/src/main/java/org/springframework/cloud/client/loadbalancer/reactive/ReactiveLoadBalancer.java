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

import org.reactivestreams.Publisher;

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
	 * @return publisher for the response
	 */
	@SuppressWarnings("rawtypes")
	Publisher<Response<T>> choose(Request request);

	default Publisher<Response<T>> choose() { // conflicting name
		return choose(REQUEST);
	}

	@FunctionalInterface
	interface Factory<T> {

		ReactiveLoadBalancer<T> getInstance(String serviceId);

	}

}
