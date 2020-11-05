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

import java.net.URI;
import java.util.Map;

import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * @author Olga Maciaszek-Sharma
 */
public final class ExchangeFilterFunctionTools {

	private ExchangeFilterFunctionTools() {
		throw new IllegalStateException("Can't instantiate a utility class.");
	}

	static String getHint(String serviceId, Map<String, String> hints) {
		String defaultHint = hints.getOrDefault("default", "default");
		String hintPropertyValue = hints.get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}

	static ClientRequest buildClientRequest(ClientRequest request, URI uri) {
		return ClientRequest.create(request.method(), uri).headers(headers -> headers.addAll(request.headers()))
				.cookies(cookies -> cookies.addAll(request.cookies()))
				.attributes(attributes -> attributes.putAll(request.attributes())).body(request.body()).build();
	}

	static String serviceInstanceUnavailableMessage(String serviceId) {
		return "LoadBalancer does not contain an instance for the service " + serviceId;
	}

}
