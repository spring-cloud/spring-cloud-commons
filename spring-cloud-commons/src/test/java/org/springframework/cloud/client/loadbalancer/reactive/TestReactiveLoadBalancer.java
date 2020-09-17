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

import java.util.Random;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;

/**
 * A sample implementation of {@link ReactiveLoadBalancer} used for tests.
 *
 * @author Olga Maciaszek-Sharma
 */
class TestReactiveLoadBalancer implements ReactiveLoadBalancer<ServiceInstance> {

	private static final String TEST_SERVICE_ID = "testServiceId";

	private final Random random = new Random();

	@Override
	public Publisher<Response<ServiceInstance>> choose() {
		return Mono.just(new DefaultResponse(new DefaultServiceInstance(TEST_SERVICE_ID, TEST_SERVICE_ID,
				TEST_SERVICE_ID, random.nextInt(40000), false)));
	}

	@Override
	public Publisher<Response<ServiceInstance>> choose(Request request) {
		return choose();
	}

}
