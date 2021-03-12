/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * A utility class for load-balanced {@link ExchangeFilterFunction} instances.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
public final class ExchangeFilterFunctionUtils {

	private ExchangeFilterFunctionUtils() {
		throw new IllegalStateException("Can't instantiate a utility class.");
	}

	static ClientRequest buildClientRequest(ClientRequest request, URI uri,
			ServiceInstance serviceInstance,
			List<LoadBalancerClientRequestTransformer> transformers) {
		ClientRequest clientRequest = ClientRequest.create(request.method(), uri)
				.headers(headers -> headers.addAll(request.headers()))
				.cookies(cookies -> cookies.addAll(request.cookies()))
				.attributes(attributes -> attributes.putAll(request.attributes()))
				.body(request.body()).build();
		if (transformers != null) {
			for (LoadBalancerClientRequestTransformer transformer : transformers) {
				clientRequest = transformer.transformRequest(clientRequest,
						serviceInstance);
			}
		}
		return clientRequest;
	}

}
