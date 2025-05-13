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

package org.springframework.cloud.client.loadbalancer;

import java.net.URI;

import org.jspecify.annotations.NonNull;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.client.service.HttpClientServiceProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.constructInterfaceClientsBaseUrl;
import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.isServiceIdUrl;

/**
 * Load-balancer-specific {@link RestClientHttpServiceGroupConfigurer} implementation. If
 * the group {@code baseUrl} is {@code null}, sets up a {@code baseUrl} with LoadBalancer
 * {@code serviceId} -resolved from Interface Client {@code groupName} set as
 * {@code host}. If the group {@code baseUrl} is {@code null} or
 * {@link LoadBalancerUriTools#isServiceIdUrl(String, String)}, a
 * {@link DeferringLoadBalancerInterceptor} instance picked from application context is
 * added to the group's {@link RestClient.Builder} if available, allowing for the requests
 * to be load-balanced.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see RestClientHttpServiceGroupConfigurer
 * @see HttpClientServiceProperties
 */
public class LoadBalancerRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 10;

	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory;

	private final SingletonSupplier<DeferringLoadBalancerInterceptor> loadBalancerInterceptorSupplier;

	private final HttpClientServiceProperties clientServiceProperties;

	public LoadBalancerRestClientHttpServiceGroupConfigurer(
			ObjectProvider<DeferringLoadBalancerInterceptor> loadBalancerInterceptorProvider,
			HttpClientServiceProperties clientServiceProperties,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		this.loadBalancerInterceptorSupplier = SingletonSupplier
			.ofNullable(loadBalancerInterceptorProvider.getIfAvailable());
		this.clientServiceProperties = clientServiceProperties;
		this.loadBalancerClientFactory = loadBalancerClientFactory;
	}

	@Override
	public void configureGroups(@NonNull Groups<RestClient.Builder> groups) {
		DeferringLoadBalancerInterceptor loadBalancerInterceptor = loadBalancerInterceptorSupplier.get();
		if (loadBalancerInterceptor == null) {
			throw new IllegalStateException(
					DeferringLoadBalancerInterceptor.class.getSimpleName() + " bean not available.");
		}
		groups.configureClient((group, builder) -> {
			String groupName = group.name();
			HttpClientServiceProperties.Group groupProperties = clientServiceProperties.getGroup().get(groupName);
			if (groupProperties == null || groupProperties.getBaseUrl() == null) {
				URI baseUrl = constructBaseUrl(groupName);
				builder.baseUrl(baseUrl);
				builder.requestInterceptor(loadBalancerInterceptor);
			}
			else if (isServiceIdUrl(groupProperties.getBaseUrl(), groupName)) {
				builder.requestInterceptor(loadBalancerInterceptor);
			}
		});

	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	private URI constructBaseUrl(String groupName) {
		LoadBalancerProperties loadBalancerProperties = loadBalancerClientFactory.getProperties(groupName);
		return constructInterfaceClientsBaseUrl(groupName,
				loadBalancerProperties.getInterfaceClients().getDefaultScheme());
	}

}
