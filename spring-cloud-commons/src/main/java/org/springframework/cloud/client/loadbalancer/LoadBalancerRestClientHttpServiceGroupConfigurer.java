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
import java.net.URISyntaxException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.jspecify.annotations.NonNull;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.client.service.HttpClientServiceProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	private static final Log LOG = LogFactory.getLog(LoadBalancerRestClientHttpServiceGroupConfigurer.class);

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
			throw new IllegalStateException(ClientHttpRequestInterceptor.class.getSimpleName() + " not available.");
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

	private boolean isServiceIdUrl(String baseUrlString, String groupName) {
		if (baseUrlString == null) {
			return false;
		}
		URI baseUrl;
		try {
			baseUrl = new URI(baseUrlString);
		}
		catch (URISyntaxException e) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Incorrect baseUrlString syntax", e);
			}
			return false;
		}
		return groupName.equals(baseUrl.getHost());
	}

	private URI constructBaseUrl(String groupName) {
		LoadBalancerProperties loadBalancerProperties = loadBalancerClientFactory.getProperties(groupName);
		return UriComponentsBuilder.newInstance()
			.scheme(loadBalancerProperties.getInterfaceClients().getDefaultScheme())
			.host(groupName)
			.encode()
			.build()
			.toUri();
	}

}
