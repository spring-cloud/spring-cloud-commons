/*
 * Copyright 2013-2020 the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A Builder for creating a {@link ServiceInstanceListSupplier} hierarchy to be used in
 * {@link ReactorLoadBalancer} configuration.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public final class ServiceInstanceListSupplierBuilder {

	private static final Log LOG = LogFactory.getLog(ServiceInstanceListSupplierBuilder.class);

	private Creator baseCreator;

	private DelegateCreator cachingCreator;

	private final List<DelegateCreator> creators = new ArrayList<>();

	ServiceInstanceListSupplierBuilder() {
	}

	/**
	 * Sets a blocking {@link DiscoveryClient}-based
	 * {@link DiscoveryClientServiceInstanceListSupplier} as a base
	 * {@link ServiceInstanceListSupplier} in the hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withBlockingDiscoveryClient() {
		if (baseCreator != null && LOG.isWarnEnabled()) {
			LOG.warn("Overriding a previously set baseCreator with a blocking DiscoveryClient baseCreator.");
		}
		this.baseCreator = context -> {
			DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);

			return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
		};
		return this;
	}

	/**
	 * Sets a {@link ReactiveDiscoveryClient}-based
	 * {@link DiscoveryClientServiceInstanceListSupplier} as a base
	 * {@link ServiceInstanceListSupplier} in the hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withDiscoveryClient() {
		if (baseCreator != null && LOG.isWarnEnabled()) {
			LOG.warn("Overriding a previously set baseCreator with a ReactiveDiscoveryClient baseCreator.");
		}
		this.baseCreator = context -> {
			ReactiveDiscoveryClient discoveryClient = context.getBean(ReactiveDiscoveryClient.class);

			return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
		};
		return this;
	}

	/**
	 * Sets a user-provided {@link ServiceInstanceListSupplier} as a base
	 * {@link ServiceInstanceListSupplier} in the hierarchy.
	 * @param supplier a user-provided {@link ServiceInstanceListSupplier} instance
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withBase(ServiceInstanceListSupplier supplier) {
		this.baseCreator = context -> supplier;
		return this;
	}

	/**
	 * Adds a {@link HealthCheckServiceInstanceListSupplier} to the
	 * {@link ServiceInstanceListSupplier} hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withHealthChecks() {
		DelegateCreator creator = (context, delegate) -> {
			LoadBalancerProperties properties = context.getBean(LoadBalancerProperties.class);
			WebClient.Builder webClient = context.getBean(WebClient.Builder.class);
			return healthCheckServiceInstanceListSupplier(webClient.build(), delegate, properties);
		};
		this.creators.add(creator);
		return this;
	}

	/**
	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
	 * {@link WebClient} instance to the {@link ServiceInstanceListSupplier} hierarchy.
	 * @param webClient a user-provided {@link WebClient} instance
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withHealthChecks(WebClient webClient) {
		DelegateCreator creator = (context, delegate) -> {
			LoadBalancerProperties properties = context.getBean(LoadBalancerProperties.class);
			return healthCheckServiceInstanceListSupplier(webClient, delegate, properties);
		};
		this.creators.add(creator);
		return this;
	}

	/**
	 * Adds a {@link SameInstancePreferenceServiceInstanceListSupplier} to the
	 * {@link ServiceInstanceListSupplier} hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withSameInstancePreference() {
		DelegateCreator creator = (context,
				delegate) -> new SameInstancePreferenceServiceInstanceListSupplier(delegate);
		this.creators.add(creator);
		return this;
	}

	/**
	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
	 * {@link RestTemplate} instance to the {@link ServiceInstanceListSupplier} hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withBlockingHealthChecks() {
		DelegateCreator creator = (context, delegate) -> {
			RestTemplate restTemplate = context.getBean(RestTemplate.class);
			LoadBalancerProperties properties = context.getBean(LoadBalancerProperties.class);
			return blockingHealthCheckServiceInstanceListSupplier(restTemplate, delegate, properties);
		};
		this.creators.add(creator);
		return this;
	}

	/**
	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
	 * {@link RestTemplate} instance to the {@link ServiceInstanceListSupplier} hierarchy.
	 * @param restTemplate a user-provided {@link RestTemplate} instance
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withBlockingHealthChecks(RestTemplate restTemplate) {
		DelegateCreator creator = (context, delegate) -> {
			LoadBalancerProperties properties = context.getBean(LoadBalancerProperties.class);
			return blockingHealthCheckServiceInstanceListSupplier(restTemplate, delegate, properties);
		};
		this.creators.add(creator);
		return this;
	}

	/**
	 * Adds a {@link ZonePreferenceServiceInstanceListSupplier} to the
	 * {@link ServiceInstanceListSupplier} hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withZonePreference() {
		DelegateCreator creator = (context, delegate) -> {
			LoadBalancerZoneConfig zoneConfig = context.getBean(LoadBalancerZoneConfig.class);
			return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig);
		};
		this.creators.add(creator);
		return this;
	}

	/**
	 * Adds a {@link RequestBasedStickySessionServiceInstanceListSupplier} to the
	 * {@link ServiceInstanceListSupplier} hierarchy.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withRequestBasedStickySession() {
		DelegateCreator creator = (context, delegate) -> {
			LoadBalancerProperties properties = context.getBean(LoadBalancerProperties.class);
			return new RequestBasedStickySessionServiceInstanceListSupplier(delegate, properties);
		};
		this.creators.add(creator);
		return this;
	}

	/**
	 * If {@link LoadBalancerCacheManager} is available in the context, wraps created
	 * {@link ServiceInstanceListSupplier} hierarchy with a
	 * {@link CachingServiceInstanceListSupplier} instance to provide a caching mechanism
	 * for service instances. Uses {@link ObjectProvider} to lazily resolve
	 * {@link LoadBalancerCacheManager}.
	 * @return the {@link ServiceInstanceListSupplierBuilder} object
	 */
	public ServiceInstanceListSupplierBuilder withCaching() {
		if (cachingCreator != null && LOG.isWarnEnabled()) {
			LOG.warn(
					"Overriding a previously set cachingCreator with a CachingServiceInstanceListSupplier-based cachingCreator.");
		}
		this.cachingCreator = (context, delegate) -> {
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceListSupplier(delegate, cacheManagerProvider.getIfAvailable());
			}
			if (LOG.isWarnEnabled()) {
				LOG.warn("LoadBalancerCacheManager not available, returning delegate without caching.");
			}
			return delegate;
		};
		return this;
	}

	public ServiceInstanceListSupplierBuilder withRetryAwareness() {
		DelegateCreator creator = (context, delegate) -> new RetryAwareServiceInstanceListSupplier(delegate);
		creators.add(creator);
		return this;
	}

	/**
	 * Builds the {@link ServiceInstanceListSupplier} hierarchy.
	 * @param context application context
	 * @return a {@link ServiceInstanceListSupplier} instance on top of the delegate
	 * hierarchy
	 */
	public ServiceInstanceListSupplier build(ConfigurableApplicationContext context) {
		Assert.notNull(baseCreator, "A baseCreator must not be null");

		ServiceInstanceListSupplier supplier = baseCreator.apply(context);

		for (DelegateCreator creator : creators) {
			supplier = creator.apply(context, supplier);
		}

		if (this.cachingCreator != null) {
			supplier = this.cachingCreator.apply(context, supplier);
		}
		return supplier;
	}

	private ServiceInstanceListSupplier healthCheckServiceInstanceListSupplier(WebClient webClient,
			ServiceInstanceListSupplier delegate, LoadBalancerProperties properties) {
		return new HealthCheckServiceInstanceListSupplier(delegate, properties.getHealthCheck(),
				(serviceInstance, healthCheckPath) -> webClient.get()
						.uri(UriComponentsBuilder.fromUri(serviceInstance.getUri()).path(healthCheckPath).build()
								.toUri())
						.exchange().flatMap(clientResponse -> clientResponse.releaseBody()
								.thenReturn(HttpStatus.OK.value() == clientResponse.rawStatusCode())));
	}

	private ServiceInstanceListSupplier blockingHealthCheckServiceInstanceListSupplier(RestTemplate restTemplate,
			ServiceInstanceListSupplier delegate, LoadBalancerProperties properties) {
		return new HealthCheckServiceInstanceListSupplier(delegate, properties.getHealthCheck(),
				(serviceInstance, healthCheckPath) -> Mono.defer(() -> {
					URI uri = UriComponentsBuilder.fromUri(serviceInstance.getUri()).path(healthCheckPath).build()
							.toUri();
					try {
						return Mono
								.just(HttpStatus.OK.equals(restTemplate.getForEntity(uri, Void.class).getStatusCode()));
					}
					catch (Exception ignored) {
						return Mono.just(false);
					}
				}));
	}

	/**
	 * Allows creating a {@link ServiceInstanceListSupplier} instance based on provided
	 * {@link ConfigurableApplicationContext}.
	 */
	public interface Creator extends Function<ConfigurableApplicationContext, ServiceInstanceListSupplier> {

	}

	/**
	 * Allows creating a {@link ServiceInstanceListSupplier} instance based on provided
	 * {@link ConfigurableApplicationContext} and another
	 * {@link ServiceInstanceListSupplier} instance that will be used as a delegate.
	 */
	public interface DelegateCreator extends
			BiFunction<ConfigurableApplicationContext, ServiceInstanceListSupplier, ServiceInstanceListSupplier> {

	}

}
