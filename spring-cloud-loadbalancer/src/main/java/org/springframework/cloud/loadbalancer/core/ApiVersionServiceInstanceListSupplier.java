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

package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

/**
 * A {@link ServiceInstanceListSupplier} that filters service instances based on the API
 * version specified in the request. The version is extracted from the request using an
 * {@link ApiVersionStrategy} and matched against the {@code VERSION} metadata field of
 * the service instances.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.x
 */
public class ApiVersionServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final String VERSION = "VERSION";

	// TODO: Construct strategy using properties
	private final ApiVersionStrategy apiVersionStrategy;

	private final boolean callGetWithRequestOnDelegates;

	public ApiVersionServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ApiVersionStrategy apiVersionStrategy, LoadBalancerClientFactory loadBalancerClientFactory) {
		super(delegate);
		this.apiVersionStrategy = apiVersionStrategy;
		callGetWithRequestOnDelegates = loadBalancerClientFactory.getProperties(getServiceId())
				.isCallGetWithRequestOnDelegates();
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		Object requestContext = request.getContext();
		if (callGetWithRequestOnDelegates && requestContext instanceof RequestDataContext requestDataContext) {
			return getDelegate().get(request)
					.map(serviceInstances -> filteredByVersion(serviceInstances,
							getVersionFromRequest(requestDataContext.getClientRequest())));
		}
		return get();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		Comparable<?> defaultVersion = apiVersionStrategy.getDefaultVersion();
		return getDelegate().get()
				.map(serviceInstances ->
						filteredByVersion(serviceInstances, defaultVersion));
	}

	private List<ServiceInstance> filteredByVersion(List<ServiceInstance> serviceInstances,
			Comparable<?> requestedVersion) {

		if (requestedVersion != null) {
			List<ServiceInstance> filteredInstances = serviceInstances.stream()
					.filter(instance -> requestedVersion.equals(getVersion(instance)))
					.toList();

			if (!filteredInstances.isEmpty()) {
				return filteredInstances;
			}
		}
		// TODO: evaluate if this logic makes sense in this scenario
		// If the version is not specified in the request or no instances match,
		// we return all instances retrieved for the given service id.
		return serviceInstances;
	}

	private Comparable<?> getVersionFromRequest(RequestData requestData) {
		ServerWebExchange exchange = buildServerWebExchange(requestData);
		return apiVersionStrategy.resolveParseAndValidateVersion(exchange);
	}

	private Comparable<?> getVersion(ServiceInstance serviceInstance) {
		Map<String, String> metadata = serviceInstance.getMetadata();
		if (metadata != null) {
			String version = metadata.get(VERSION);
			if (version != null) {
				return apiVersionStrategy.parseVersion(version);
			}
		}
		return null;
	}

	// FIXME: if FW adjusted, generate ServerHttpRequest only
	private static ServerWebExchange buildServerWebExchange(RequestData requestData) {
		ServerHttpRequest serverRequest = new LoadBalancerServerHttpRequest(
				requestData);
		ServerHttpResponse serverResponse = new EmptyServerHttpResponse();
		return new DefaultServerWebExchange(serverRequest,
				serverResponse, new DefaultWebSessionManager(), new DefaultServerCodecConfigurer(),
				new AcceptHeaderLocaleContextResolver());
	}

}
