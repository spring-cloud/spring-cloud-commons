/*
 * Copyright 2025-present the original author or authors.
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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.reactive.accept.ApiVersionResolver;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.reactive.accept.MediaTypeParamApiVersionResolver;
import org.springframework.web.reactive.accept.PathApiVersionResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

/**
 * A reactive version of the {@link ServiceInstanceListSupplier} that filters service
 * instances based on the API version specified in the request. The version is extracted
 * from the request using an {@link ApiVersionStrategy} and matched against the
 * {@link ReactiveApiVersionServiceInstanceListSupplier#API_VERSION} metadata field of the
 * service instances.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.x
 */
public class ReactiveApiVersionServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	/**
	 * Service instance metadata map key that service instance API version is retrieved
	 * from.
	 */
	public static final String API_VERSION = "API_VERSION";

	private static final Log LOG = LogFactory.getLog(ReactiveApiVersionServiceInstanceListSupplier.class);

	private final boolean callGetWithRequestOnDelegates;

	private final LoadBalancerProperties.ApiVersion apiVersionProperties;

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	private @Nullable ApiVersionParser<?> apiVersionParser;

	private @Nullable ApiVersionStrategy apiVersionStrategy;

	public ReactiveApiVersionServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		super(delegate);
		String serviceId = getServiceId();
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		LoadBalancerProperties properties = loadBalancerClientFactory.getProperties(serviceId);
		if (properties != null) {
			callGetWithRequestOnDelegates = properties.isCallGetWithRequestOnDelegates();
			apiVersionProperties = properties.getApiVersion();
		}
		else {
			callGetWithRequestOnDelegates = true;
			apiVersionProperties = new LoadBalancerProperties.ApiVersion();
		}
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
		Comparable<?> defaultVersion = getApiVersionStrategy().getDefaultVersion();
		return getDelegate().get().map(serviceInstances -> filteredByVersion(serviceInstances, defaultVersion));
	}

	private List<ServiceInstance> filteredByVersion(List<ServiceInstance> serviceInstances,
			@Nullable Comparable<?> requestedVersion) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Matching instances by API Version: " + requestedVersion);
		}

		if (requestedVersion != null) {
			List<ServiceInstance> filteredInstances = serviceInstances.parallelStream()
				.filter(instance -> requestedVersion.equals(getVersion(instance)))
				.toList();

			if (!filteredInstances.isEmpty()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Found matching instances by API Version: " + filteredInstances);
				}
				return filteredInstances;
			}
		}
		if (apiVersionProperties.isFallbackToAvailableInstances()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No matching instances found by API Version: " + requestedVersion
						+ ". Falling back to all available instances.");
			}
			return serviceInstances;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("No matching instances found by API Version: " + requestedVersion + ". Returning empty list.");
		}
		return List.of();
	}

	// Visible for tests
	void setApiVersionParser(ApiVersionParser<?> apiVersionParser) {
		this.apiVersionParser = apiVersionParser;
	}

	private @Nullable Comparable<?> getVersionFromRequest(@Nullable RequestData requestData) {
		if (requestData == null) {
			return null;
		}
		ServerWebExchange exchange = buildServerWebExchange(requestData);
		Comparable<?> apiVersion = getApiVersionStrategy().resolveParseAndValidateVersion(exchange);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Resolved Api Version from request: " + apiVersion);
		}
		return apiVersion;
	}

	private @Nullable Comparable<?> getVersion(ServiceInstance serviceInstance) {
		Map<String, String> metadata = serviceInstance.getMetadata();
		if (metadata != null) {
			String version = metadata.get(API_VERSION);
			if (version != null) {
				return getApiVersionStrategy().parseVersion(version);
			}
		}
		return null;
	}

	private static ServerWebExchange buildServerWebExchange(RequestData requestData) {
		ServerHttpRequest serverRequest = new LoadBalancerServerHttpRequest(requestData);
		ServerHttpResponse serverResponse = new EmptyServerHttpResponse();
		return new DefaultServerWebExchange(serverRequest, serverResponse, new DefaultWebSessionManager(),
				new DefaultServerCodecConfigurer(), new AcceptHeaderLocaleContextResolver());
	}

	@SuppressWarnings("rawtypes")
	private @Nullable ApiVersionParser getApiVersionParser() {
		if (apiVersionParser == null) {
			apiVersionParser = loadBalancerClientFactory.getInstance(getServiceId(), ApiVersionParser.class);
		}
		return apiVersionParser;
	}

	private ApiVersionStrategy getApiVersionStrategy() {
		if (apiVersionStrategy == null) {
			ApiVersionStrategy userProvidedApiVersionStrategy = loadBalancerClientFactory.getInstance(getServiceId(),
					ApiVersionStrategy.class);
			apiVersionStrategy = userProvidedApiVersionStrategy != null ? userProvidedApiVersionStrategy
					: buildApiVersionStrategy();
		}
		return apiVersionStrategy;
	}

	@SuppressWarnings("NullAway") // guarded by hasText()
	private ApiVersionStrategy buildApiVersionStrategy() {
		List<ApiVersionResolver> versionResolvers = new ArrayList<>();

		if (StringUtils.hasText(apiVersionProperties.getHeader())) {
			versionResolvers
				.add(exchange -> exchange.getRequest().getHeaders().getFirst(apiVersionProperties.getHeader()));
		}
		if (StringUtils.hasText(apiVersionProperties.getQueryParameter())) {
			versionResolvers.add(exchange -> exchange.getRequest()
				.getQueryParams()
				.getFirst(apiVersionProperties.getQueryParameter()));
		}
		if (apiVersionProperties.getPathSegment() != null) {
			versionResolvers.add(new PathApiVersionResolver(apiVersionProperties.getPathSegment()));
		}
		apiVersionProperties.getMediaTypeParameters()
			.forEach((mediaType, paramName) -> versionResolvers
				.add(new MediaTypeParamApiVersionResolver(mediaType, paramName)));

		return new ReactiveLoadBalancerApiVersionStrategy(versionResolvers, getApiVersionParser(),
				apiVersionProperties.getRequired(), apiVersionProperties.getDefaultVersion(), false, null, null);
	}

}
