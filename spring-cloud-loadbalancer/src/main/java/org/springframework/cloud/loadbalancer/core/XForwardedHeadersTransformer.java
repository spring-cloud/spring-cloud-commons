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

package org.springframework.cloud.loadbalancer.core;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * To add X-Forwarded-Host and X-Forwarded-Proto Headers.
 *
 * @author Gandhimathi Velusamy
 * @since 3.1.0
 */

public class XForwardedHeadersTransformer implements LoadBalancerClientRequestTransformer {

	private final LoadBalancerProperties.XForwarded xForwarded;

	public XForwardedHeadersTransformer(LoadBalancerProperties.XForwarded xForwarded) {
		this.xForwarded = xForwarded;
	}

	@Override
	public ClientRequest transformRequest(ClientRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		if (xForwarded.isEnabled()) {
			HttpHeaders headers = request.headers();
			String xForwardedHost = request.url().getHost();
			String xForwardedProto = request.url().getScheme();
			headers.add("X-Forwarded-Host", xForwardedHost);
			headers.add("X-Forwarded-Proto", xForwardedProto);
		}
		return request;
	}

}
