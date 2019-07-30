/*
 * Copyright 2012-2019 the original author or authors.
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

import java.net.URI;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Represents a <a href="https://projectreactor.io/">Project-Reactor</a>-based client-side
 * load balancer.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */

public interface ReactorLoadBalancerClient {

	/**
	 * Chooses a {@link ServiceInstance} instance based on serviceId and request.
	 * @param serviceId id of the service to select the instance for
	 * @param request request providing
	 * @return a {@link Mono} of load balancer {@link Response} containing selected
	 * {@link ServiceInstance}
	 */
	Mono<Response<ServiceInstance>> choose(String serviceId, Request request);

	/**
	 * Chooses a {@link ServiceInstance} instance based on serviceId.
	 * @param serviceId id of the service to select the instance for
	 * @return a {@link Mono} of load balancer {@link Response} containing selected
	 * {@link ServiceInstance}
	 */
	Mono<Response<ServiceInstance>> choose(String serviceId);

	/**
	 * Reconstructs the {@link URI} in a way that will result in passing the request to
	 * the provided {@link ServiceInstance}.
	 * @param serviceInstance to direct the request to
	 * @param original the original {@link URI}
	 * @return a {@link Mono} of the reconstructed {@link URI}
	 */
	Mono<URI> reconstructURI(ServiceInstance serviceInstance, URI original);

}
