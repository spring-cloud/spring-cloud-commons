/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

import java.net.URI;

import org.jspecify.annotations.NonNull;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.DEFAULT_SCHEME;
import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.constructInterfaceClientsBaseUrl;

/**
 * Load-balancer-specific {@link RestClientHttpServiceGroupConfigurer} implementation. If
 * the group {@code baseUrl} is {@code null}, sets up a {@code baseUrl} with LoadBalancer
 * {@code serviceId} -resolved from HTTP Service Client {@code groupName} set as
 * {@code host}. If the group {@code baseUrl} is {@code null} or has {@code lb} set as its
 * scheme, a {@link DeferringLoadBalancerInterceptor} instance picked from application
 * context is added to the group's {@link RestClient.Builder} if available, allowing for
 * the requests to be load-balanced.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see RestClientHttpServiceGroupConfigurer
 * @see HttpServiceClientProperties
 */
public class LoadBalancerRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 10;

	private final SingletonSupplier<DeferringLoadBalancerInterceptor> loadBalancerInterceptorSupplier;

	private final HttpServiceClientProperties clientServiceProperties;

	public LoadBalancerRestClientHttpServiceGroupConfigurer(
			ObjectProvider<DeferringLoadBalancerInterceptor> loadBalancerInterceptorProvider,
			HttpServiceClientProperties clientServiceProperties) {
		this.loadBalancerInterceptorSupplier = SingletonSupplier
			.ofNullable(loadBalancerInterceptorProvider::getIfAvailable);
		this.clientServiceProperties = clientServiceProperties;
	}

	@Override
	public void configureGroups(@NonNull Groups<RestClient.Builder> groups) {
		DeferringLoadBalancerInterceptor loadBalancerInterceptor = loadBalancerInterceptorSupplier.get();
		if (loadBalancerInterceptor == null) {
			throw new IllegalStateException(
					DeferringLoadBalancerInterceptor.class.getSimpleName() + " bean not available.");
		}

		groups.forEachGroup((group, clientBuilder, factoryBuilder) -> {
			String groupName = group.name();
			HttpClientProperties groupProperties = clientServiceProperties.get(groupName);
			String baseUrlString = groupProperties == null ? null : groupProperties.getBaseUrl();
			URI existingBaseUrl = baseUrlString == null ? null : URI.create(baseUrlString);
			if (existingBaseUrl == null) {
				URI baseUrl = constructBaseUrl(groupName);
				clientBuilder.baseUrl(baseUrl);
				clientBuilder.requestInterceptor(loadBalancerInterceptor);
			}
			else if ("lb".equalsIgnoreCase(existingBaseUrl.getScheme())) {
				URI baseUrl = UriComponentsBuilder.fromUri(existingBaseUrl).scheme(DEFAULT_SCHEME).build().toUri();
				clientBuilder.baseUrl(baseUrl);
				clientBuilder.requestInterceptor(loadBalancerInterceptor);
			}
		});
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	private URI constructBaseUrl(String groupName) {
		return constructInterfaceClientsBaseUrl(groupName);
	}

}
