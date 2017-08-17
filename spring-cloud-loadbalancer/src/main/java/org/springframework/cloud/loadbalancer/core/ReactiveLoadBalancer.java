/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import reactor.core.publisher.Mono;

/**
 * @author Spencer Gibb
 */
public interface ReactiveLoadBalancer<T> {

	/**
	 * Response created for each request.
 	 */
	interface Response<T> {
		// boolean hasServer(); TODO: needed in reactive?

		Mono<T> getServer();

		/**
		 * Notification that the request completed
		 * @param onComplete
		 */
		Mono<Void> onComplete(OnComplete onComplete);
	}

	interface Request {
		//TODO: define contents
	}

	/**
	 * Choose the next server based on the load balancing algorithm
	 * @param request
	 * @return
	 */
	Mono<Response<T>> choose(Request request);
}
