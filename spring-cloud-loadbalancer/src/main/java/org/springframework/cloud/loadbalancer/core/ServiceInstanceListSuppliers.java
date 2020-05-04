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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A utility class providing a {@link Builder} for creating a {@link ServiceInstanceListSupplier}
 * hierarchy to be used in {@link ReactorLoadBalancer} configuration.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public abstract class ServiceInstanceListSuppliers {

	private ServiceInstanceListSuppliers() {

	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Allows creating a {@link ServiceInstanceListSupplier} instance based on provided {@link ConfigurableApplicationContext}.
	 */
	public interface Creator extends Function<ConfigurableApplicationContext, ServiceInstanceListSupplier> {

	}

	/**
	 * Allows creating a {@link ServiceInstanceListSupplier} instance based on provided {@link ConfigurableApplicationContext}
	 * and another {@link ServiceInstanceListSupplier} instance that will be used as a delegate.
	 */
	public interface DelegateCreator extends BiFunction<ConfigurableApplicationContext, ServiceInstanceListSupplier, ServiceInstanceListSupplier> {

	}

	/**
	 * A builder for creating a {@link ServiceInstanceListSupplier} hierarchy to be used in
	 * {@link ReactorLoadBalancer} configuration.
	 */
	public static class Builder {

		private static final Log LOG = LogFactory.getLog(Builder.class);

		private Creator baseCreator;
		private DelegateCreator cachingCreator;
		private final List<DelegateCreator> creators = new ArrayList<>();

		public Builder() {
		}

		/**
		 * Sets a blocking {@link DiscoveryClient}-based {@link DiscoveryClientServiceInstanceListSupplier} as a base {@link ServiceInstanceListSupplier}
		 * in the hierarchy.
		 * @return the {@link Builder} object
		 */
		public Builder withBlockingDiscoveryClient() {
			if (baseCreator != null && LOG.isWarnEnabled()) {
				LOG.warn("Overriding a previously set baseCreator with a blocking DiscoveryClient baseCreator.");
			}
			this.baseCreator = context -> {
				DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);

				return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context
						.getEnvironment());
			};
			return this;
		}

		/**
		 * Sets a {@link ReactiveDiscoveryClient}-based {@link DiscoveryClientServiceInstanceListSupplier} as a base {@link ServiceInstanceListSupplier}
		 * in the hierarchy.
		 * @return the {@link Builder} object
		 */
		public Builder withDiscoveryClient() {
			if (baseCreator != null && LOG.isWarnEnabled()) {
				LOG.warn("Overriding a previously set baseCreator with a ReactiveDiscoveryClient baseCreator.");
			}
			this.baseCreator = context -> {
				ReactiveDiscoveryClient discoveryClient = context
						.getBean(ReactiveDiscoveryClient.class);

				return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context
						.getEnvironment());
			};
			return this;
		}

		/**
		 * Sets a user-provided {@link ServiceInstanceListSupplier} as a base
		 * {@link ServiceInstanceListSupplier} in the hierarchy.
		 * @param supplier a user-provided {@link ServiceInstanceListSupplier} instance
		 * @return the {@link Builder} object
		 */
		public Builder withBase(ServiceInstanceListSupplier supplier) {
			this.baseCreator = context -> supplier;
			return this;
		}

		/**
		 * Adds a {@link HealthCheckServiceInstanceListSupplier} to the
		 * {@link ServiceInstanceListSupplier} hierarchy.
		 * @return the {@link Builder} object
		 */
		public Builder withHealthChecks() {
			DelegateCreator creator = (context, delegate) -> {
				LoadBalancerProperties properties = context
						.getBean(LoadBalancerProperties.class);
				WebClient.Builder webClient = context.getBean(WebClient.Builder.class);
				return new HealthCheckServiceInstanceListSupplier(delegate, properties
						.getHealthCheck(), webClient.build());
			};
			this.creators.add(creator);
			return this;
		}

		/**
		 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided {@link WebClient} instance
		 * to the {@link ServiceInstanceListSupplier} hierarchy.
		 * @param webClient a user-provided {@link WebClient} instance
		 * @return the {@link Builder} object
		 */
		public Builder withHealthChecks(WebClient webClient) {
			DelegateCreator creator = (context, delegate) -> {
				LoadBalancerProperties properties = context
						.getBean(LoadBalancerProperties.class);
				return new HealthCheckServiceInstanceListSupplier(delegate, properties
						.getHealthCheck(), webClient);
			};
			this.creators.add(creator);
			return this;
		}

		/**
		 * Adds a {@link ZonePreferenceServiceInstanceListSupplier} to the
		 * {@link ServiceInstanceListSupplier} hierarchy.
		 * @return the {@link Builder} object
		 */
		public Builder withZonePreference() {
			DelegateCreator creator = (context, delegate) -> {
				LoadBalancerZoneConfig zoneConfig = context
						.getBean(LoadBalancerZoneConfig.class);
				return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig);
			};
			this.creators.add(creator);
			return this;
		}

		/**
		 * Wraps created {@link ServiceInstanceListSupplier} hierarchy with a {@link CachingServiceInstanceListSupplier}
		 * instance to provide a caching mechanism for service instances.
		 * @return the {@link Builder} object
		 */
		public Builder withCaching() {
			if (cachingCreator != null && LOG.isWarnEnabled()) {
				LOG.warn("Overriding a previously set cachingCreator with a CachingServiceInstanceListSupplier-based cachingCreator.");
			}
			this.cachingCreator = (context, delegate) -> {
				CacheManager cacheManager = context.getBean(CacheManager.class);

				return new CachingServiceInstanceListSupplier(delegate, cacheManager);
			};
			return this;
		}

		/**
		 * Builds the {@link ServiceInstanceListSupplier} hierarchy.
		 * @param context application context
		 * @return a {@link ServiceInstanceListSupplier} instance on top of the delegate hierarchy
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

	}
}
