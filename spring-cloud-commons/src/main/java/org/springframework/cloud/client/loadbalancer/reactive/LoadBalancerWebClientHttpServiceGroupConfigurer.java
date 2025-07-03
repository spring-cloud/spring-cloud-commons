/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.restclient.autoconfigure.service.HttpClientServiceProperties;
import org.springframework.boot.webclient.autoconfigure.service.ReactiveHttpClientServiceProperties;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.DEFAULT_SCHEME;
import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.constructInterfaceClientsBaseUrl;

/**
 * Load-balancer-specific {@link WebClientHttpServiceGroupConfigurer} implementation. If
 * the group {@code baseUrl} is {@code null}, sets up a {@code baseUrl} with LoadBalancer
 * {@code serviceId} -resolved from Interface Client {@code groupName} set as
 * {@code host}. If the group {@code baseUrl} is {@code null} or has {@code lb} set as its
 * scheme, a {@link DeferringLoadBalancerExchangeFilterFunction} instance picked from
 * application context is added to the group's {@link WebClient.Builder} if available,
 * allowing for the requests to be load-balanced.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see WebClient.Builder
 * @see HttpClientServiceProperties
 */
public class LoadBalancerWebClientHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 10;

	private final SingletonSupplier<DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction>> loadBalancerFilterFunctionSupplier;

	private final ReactiveHttpClientServiceProperties clientServiceProperties;

	public LoadBalancerWebClientHttpServiceGroupConfigurer(
			ObjectProvider<DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction>> exchangeFilterFunctionProvider,
			ReactiveHttpClientServiceProperties clientServiceProperties) {
		this.loadBalancerFilterFunctionSupplier = SingletonSupplier
			.ofNullable(exchangeFilterFunctionProvider::getIfAvailable);
		this.clientServiceProperties = clientServiceProperties;
	}

	@Override
	public void configureGroups(Groups<WebClient.Builder> groups) {
		DeferringLoadBalancerExchangeFilterFunction<LoadBalancedExchangeFilterFunction> loadBalancerFilterFunction = loadBalancerFilterFunctionSupplier
			.get();
		if (loadBalancerFilterFunction == null) {
			throw new IllegalStateException(
					DeferringLoadBalancerExchangeFilterFunction.class.getSimpleName() + " bean not available.");
		}
		groups.forEachGroup((group, clientBuilder, factoryBuilder) -> {
			String groupName = group.name();
			ReactiveHttpClientServiceProperties.Group groupProperties = clientServiceProperties.getGroup()
				.get(groupName);
			String baseUrlString = groupProperties == null ? null : groupProperties.getBaseUrl();
			URI existingBaseUrl = baseUrlString == null ? null : URI.create(baseUrlString);
			if (existingBaseUrl == null) {
				URI baseUrl = constructBaseUrl(groupName);
				clientBuilder.baseUrl(String.valueOf(baseUrl));
				clientBuilder.filter(loadBalancerFilterFunction);
			}
			else if ("lb".equalsIgnoreCase(existingBaseUrl.getScheme())) {
				String baseUrl = UriComponentsBuilder.fromUri(existingBaseUrl)
					.scheme(DEFAULT_SCHEME)
					.build()
					.toUriString();
				clientBuilder.baseUrl(baseUrl);
				clientBuilder.filter(loadBalancerFilterFunction);
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
