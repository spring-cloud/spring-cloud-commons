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
import java.util.Random;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Olga Maciaszek-Sharma
 */
class TestReactorLoadBalancerClient implements ReactorLoadBalancerClient {

	private final Random random = new Random();

	@Override
	public Mono<Response<ServiceInstance>> choose(String serviceId, Request request) {
		return choose(serviceId);
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(String serviceId) {
		return Mono.just(new DefaultResponse(new DefaultServiceInstance(serviceId,
				serviceId, serviceId, random.nextInt(40000), false)));
	}

	@Override
	public Mono<URI> reconstructURI(ServiceInstance instance, URI original) {
		return Mono.just(DefaultServiceInstance.getUri(instance));
	}

}
