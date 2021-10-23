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

package org.springframework.cloud.loadbalancer.blocking;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

/**
 * To add X-Forward-Host and X-Forward-Proto Headers.
 *
 * @author Gandhimathi Velusamy
 */

public class XForwarderHeadersTransformer implements LoadBalancerRequestTransformer {

	private final LoadBalancerProperties.Xforwarded xforwardedHeaders;

	public XForwarderHeadersTransformer(LoadBalancerProperties.Xforwarded xforwardedHeaders) {
		this.xforwardedHeaders = xforwardedHeaders;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		if (xforwardedHeaders.isEnabledXforwarded()) {
			HttpHeaders headers = request.getHeaders();
			String xforwardedHost = "";
			xforwardedHost += request.getURI().getHost();
			String xforwardedProto = request.getURI().getScheme();
			headers.add("X-Forwarded-Host", xforwardedHost);
			headers.add("X-Forwarded-Proto", xforwardedProto);
		}
		return request;
	}

}
