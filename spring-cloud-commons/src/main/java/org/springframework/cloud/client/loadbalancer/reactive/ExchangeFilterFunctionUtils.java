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
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * A utility class for load-balanced {@link ExchangeFilterFunction} instances.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public final class ExchangeFilterFunctionUtils {

	private ExchangeFilterFunctionUtils() {
		throw new IllegalStateException("Can't instantiate a utility class.");
	}

	static String getHint(String serviceId, Map<String, String> hints) {
		String defaultHint = hints.getOrDefault("default", "default");
		String hintPropertyValue = hints.get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}

	static ClientRequest buildClientRequest(ClientRequest request, ServiceInstance serviceInstance,
			String instanceIdCookieName, boolean addServiceInstanceCookie,
			List<LoadBalancerClientRequestTransformer> transformers) {
		URI originalUrl = request.url();
		ClientRequest clientRequest = ClientRequest
				.create(request.method(), LoadBalancerUriTools.reconstructURI(serviceInstance, originalUrl))
				.headers(headers -> headers.addAll(request.headers())).cookies(cookies -> {
					cookies.addAll(request.cookies());
					if (!(instanceIdCookieName == null || instanceIdCookieName.length() == 0)
							&& addServiceInstanceCookie) {
						cookies.add(instanceIdCookieName, serviceInstance.getInstanceId());
					}
				}).attributes(attributes -> attributes.putAll(request.attributes())).body(request.body()).build();
		if (transformers != null) {
			for (LoadBalancerClientRequestTransformer transformer : transformers) {
				clientRequest = transformer.transformRequest(clientRequest, serviceInstance);
			}
		}
		return clientRequest;
	}

	static String serviceInstanceUnavailableMessage(String serviceId) {
		return "LoadBalancer does not contain an instance for the service " + serviceId;
	}

}
