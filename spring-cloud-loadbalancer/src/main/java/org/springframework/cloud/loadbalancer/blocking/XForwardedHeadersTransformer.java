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
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

/**
 * To add X-Forwarded-Host and X-Forwarded-Proto Headers.
 *
 * @author Gandhimathi Velusamy
 * @author Olga Maciaszek-Sharma
 * @author junjie shen(沈俊杰)
 * @since 3.1.0
 */

public class XForwardedHeadersTransformer implements LoadBalancerRequestTransformer {

	private final ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	public XForwardedHeadersTransformer(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
		this.factory = factory;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		LoadBalancerProperties.XForwarded xForwarded = factory.getProperties(instance.getServiceId()).getXForwarded();
		if (xForwarded.isEnabled()) {
			HttpHeaders headers = request.getHeaders();
			String xForwardedHost = request.getURI().getHost();
			String xForwardedProto = request.getURI().getScheme();
			headers.add("X-Forwarded-Host", xForwardedHost);
			headers.add("X-Forwarded-Proto", xForwardedProto);
		}
		return request;
	}

}
