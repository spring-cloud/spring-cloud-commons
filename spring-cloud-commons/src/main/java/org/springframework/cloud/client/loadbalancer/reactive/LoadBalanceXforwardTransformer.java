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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * To add X-Forward-Host and X-Forward-Proto headers.
 *
 * @author gandhimathi
 */

public class LoadBalanceXforwardTransformer implements LoadBalancerClientRequestTransformer {

	private LoadBalancerProperties.Xforwarded xforwardedHeaders;

	@Override
	public ClientRequest transformRequest(ClientRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}

		if (xforwardedHeaders.isEnableXforwarded()) {
			HttpHeaders headers = request.headers();

			String xforwardedHost = "";
			xforwardedHost += request.url().getHost();
			String xproto = request.url().getScheme();
			headers.add("X-Forwarded-host", xforwardedHost);
			headers.add("X-Forwarded-proto", xproto);
		}
		return request;
	}

}
