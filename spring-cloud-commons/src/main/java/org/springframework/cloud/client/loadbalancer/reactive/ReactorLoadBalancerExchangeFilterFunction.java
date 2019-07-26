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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ReactorLoadBalancerExchangeFilterFunction implements ExchangeFilterFunction {

	private static Log logger = LogFactory
			.getLog(LoadBalancerExchangeFilterFunction.class);

	private final ReactorLoadBalancerClient loadBalancerClient;

	public ReactorLoadBalancerExchangeFilterFunction(ReactorLoadBalancerClient loadBalancerClient) {
		this.loadBalancerClient = loadBalancerClient;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		URI originalUrl = request.url();
		String serviceId = originalUrl.getHost();
		if (serviceId == null) {
			String msg = String
					.format("Request URI does not contain a valid hostname: %s", originalUrl
							.toString());
			logger.warn(msg);
			return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).body(msg)
					.build());
		}
		return loadBalancerClient.choose(serviceId).log().handle((response, sink) -> {
			ServiceInstance instance = response.getServer();
			if (instance == null) {
				String message = getServiceInstanceUnavailableMessage(serviceId);
				logger.warn(message);
				sink.error(new IllegalStateException(message));
			}
			else {
				logger.debug(String
						.format("Load balancer has retrieved the instance for service %s: %s", serviceId,
								instance.getUri()));
				sink.next(instance);
			}
		}).flatMap(serviceInstance -> loadBalancerClient
				.reconstructURI((ServiceInstance) serviceInstance, originalUrl))
				.map(uri -> buildClientRequest(request, uri)).flatMap(next::exchange)
				.onErrorReturn(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
						.body(getServiceInstanceUnavailableMessage(serviceId)).build());
	}

	private String getServiceInstanceUnavailableMessage(String serviceId) {
		return "Load balancer does not contain an instance for the service " + serviceId;
	}

	private ClientRequest buildClientRequest(ClientRequest request, URI uri) {
		return ClientRequest.create(request.method(), uri)
				.headers(headers -> headers.addAll(request.headers()))
				.cookies(cookies -> cookies.addAll(request.cookies()))
				.attributes(attributes -> attributes.putAll(request.attributes()))
				.body(request.body()).build();
	}

}
