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

import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class ServiceInstanceListSuppliers {

	private ServiceInstanceListSuppliers() {

	}

	public static Builder builder() {
		return new Builder();
	}

	public interface Creator extends Function<ConfigurableApplicationContext, ServiceInstanceListSupplier> {

	}

	public interface DelegateCreator extends BiFunction<ConfigurableApplicationContext, ServiceInstanceListSupplier, ServiceInstanceListSupplier> {

	}

	public static class Builder {

		private Creator baseCreator;
		private DelegateCreator cachingCreator;
		private List<DelegateCreator> creators = new ArrayList<>();

		public Builder() {
		}

		public Builder withBlockingDiscoveryClient() {
			//TODO: warn if baseCreator is not null
			this.baseCreator = context -> {
				DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);

				return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
			};
			return this;
		}

		public Builder withDiscoveryClient() {
			//TODO: warn if baseCreator is not null
			this.baseCreator = context -> {
				ReactiveDiscoveryClient discoveryClient = context.getBean(ReactiveDiscoveryClient.class);

				return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
			};
			return this;
		}

		public Builder withBase(ServiceInstanceListSupplier supplier) {
			this.baseCreator = context -> supplier;
			return this;
		}

		//TODO: overload with WebClient?
		public Builder withHealthChecks() {
			DelegateCreator creator = (context, delegate) -> {
				LoadBalancerProperties properties = context.getBean(LoadBalancerProperties.class);
				WebClient.Builder webClient = context.getBean(WebClient.Builder.class);
				return new HealthCheckServiceInstanceListSupplier(delegate, properties.getHealthCheck(), webClient.build());
			};
			this.creators.add(creator);
			return this;
		}

		public Builder withZonePreference() {
			DelegateCreator creator = (context, delegate) -> {
				LoadBalancerZoneConfig zoneConfig = context.getBean(LoadBalancerZoneConfig.class);
				return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig);
			};
			this.creators.add(creator);
			return this;
		}

		public Builder withCaching() {
			//TODO: warn if baseCreator is not null
			this.cachingCreator = (context, delegate) -> {
				CacheManager cacheManager = context.getBean(CacheManager.class);

				return new CachingServiceInstanceListSupplier(delegate, cacheManager);
			};
			return this;
		}

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
