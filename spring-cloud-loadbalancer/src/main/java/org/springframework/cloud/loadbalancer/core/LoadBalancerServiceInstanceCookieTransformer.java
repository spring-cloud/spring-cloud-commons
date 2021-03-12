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

package org.springframework.cloud.loadbalancer.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.StringUtils;

/**
 * A {@link LoadBalancerRequestTransformer} that allows passing the {@code} instanceId) of
 * the {@link ServiceInstance} selected by the {@link LoadBalancerClient} in a cookie.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.2
 */
public class LoadBalancerServiceInstanceCookieTransformer implements LoadBalancerRequestTransformer {

	private final LoadBalancerProperties.StickySession stickySessionProperties;

	public LoadBalancerServiceInstanceCookieTransformer(LoadBalancerProperties.StickySession stickySessionProperties) {
		this.stickySessionProperties = stickySessionProperties;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		String instanceIdCookieName = stickySessionProperties.getInstanceIdCookieName();
		if (!StringUtils.hasText(instanceIdCookieName)) {
			return request;
		}
		HttpHeaders headers = request.getHeaders();
		List<String> cookieHeaders = new ArrayList<>(request.getHeaders().getOrEmpty(HttpHeaders.COOKIE));
		String serviceInstanceCookie = new HttpCookie(instanceIdCookieName, instance.getInstanceId()).toString();
		cookieHeaders.add(serviceInstanceCookie);
		headers.put(HttpHeaders.COOKIE, cookieHeaders);
		return request;
	}

}
