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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Tests for {@link ReactorLoadBalancerExchangeFilterFunction}.
 *
 * @author Olga Maciaszek-Sharma
 */
// TODO: implement!
class ReactorLoadBalancerExchangeFilterFunctionTests {

	@Test
	void exchangeFunctionReturnsDefaultResponseWhenServiceInstanceMissing() {
		Hooks.onOperatorDebug();
		WebClient webClient = WebClient.builder().baseUrl(null)
				.filter(new ReactorLoadBalancerExchangeFilterFunction(
						new TestReactorLoadBalancerClient()))
				.build();
		webClient.get().uri("/xxx").retrieve().bodyToMono(String.class).block();
		assert true;

	}

}

class TestReactorLoadBalancerClient implements ReactorLoadBalancerClient {

	@Override
	public Mono<Response<ServiceInstance>> choose(String serviceId, Request request) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(String serviceId) {
		return Mono.just(new DefaultResponse(null));
	}

	@Override
	public Mono<URI> reconstructURI(ServiceInstance serviceInstance, URI original) {
		if (serviceInstance == null) {
			return Mono.defer(() -> Mono.error(
					new IllegalArgumentException("Service Instance cannot be null.")));
		}
		return Mono.just(URI.create("http://test.example"));
	}

}

class TestServiceInstance implements ServiceInstance {

	private URI uri;

	private String scheme = "http";

	private String host = "test.example";

	private int port = 8080;

	private boolean secure;

	private Map<String, String> metadata = new LinkedHashMap<>();

	TestServiceInstance withScheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	TestServiceInstance withPort(int port) {
		this.port = port;
		return this;
	}

	TestServiceInstance withSecure(boolean secure) {
		this.secure = secure;
		return this;
	}

	@Override
	public String getServiceId() {
		return "test-service";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public String getScheme() {
		return scheme;
	}

}
