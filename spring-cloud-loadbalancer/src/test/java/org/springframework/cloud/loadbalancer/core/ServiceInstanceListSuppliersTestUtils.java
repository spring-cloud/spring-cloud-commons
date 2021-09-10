/*
 * Copyright 2013-2020 the original author or authors.
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

import java.net.URI;
import java.util.function.BiFunction;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplierBuilder.getUri;

/**
 * A utility class for {@link ServiceInstanceListSupplier} tests.
 *
 * @author Olga Maciaszek-Sharma
 * @author Sabyasachi Bhattacharya
 * @since 3.0.0
 */
final class ServiceInstanceListSuppliersTestUtils {

	private ServiceInstanceListSuppliersTestUtils() {
		throw new UnsupportedOperationException("Cannot instantiate utility class");
	}

	static BiFunction<ServiceInstance, String, Mono<Boolean>> healthCheckFunction(WebClient webClient) {
		return (serviceInstance, healthCheckPath) -> webClient.get()
				.uri(UriComponentsBuilder.fromUriString(getUri(serviceInstance, healthCheckPath)).build().toUri())
				.exchange().flatMap(clientResponse -> clientResponse.releaseBody()
						.thenReturn(HttpStatus.OK.value() == clientResponse.rawStatusCode()));
	}

	static BiFunction<ServiceInstance, String, Mono<Boolean>> healthCheckFunction(RestTemplate restTemplate) {
		return (serviceInstance, healthCheckPath) -> Mono.defer(() -> {
			URI uri = UriComponentsBuilder.fromUriString(getUri(serviceInstance, healthCheckPath)).build().toUri();
			try {
				return Mono.just(HttpStatus.OK.equals(restTemplate.getForEntity(uri, Void.class).getStatusCode()));
			}
			catch (Exception ignored) {
				return Mono.just(false);
			}
		});
	}

}
