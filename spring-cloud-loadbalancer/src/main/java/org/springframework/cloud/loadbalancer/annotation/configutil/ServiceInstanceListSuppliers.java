/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.loadbalancer.annotation.configutil;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.HealthCheckServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utility class containing helper methods for setting up
 * {@link ServiceInstanceListSupplier} configuration in a more concise way.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.3
 */
public final class ServiceInstanceListSuppliers {

	private ServiceInstanceListSuppliers() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static ServiceInstanceListSupplier cachedOrDelegate(
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider,
			ServiceInstanceListSupplier delegate) {
		if (cacheManagerProvider.getIfAvailable() != null) {
			return new CachingServiceInstanceListSupplier(delegate,
					cacheManagerProvider.getIfAvailable());
		}
		return delegate;
	}

	public static ServiceInstanceListSupplier cachedOrDelegate(ApplicationContext context,
			ServiceInstanceListSupplier delegate) {
		return cachedOrDelegate(context.getBeanProvider(LoadBalancerCacheManager.class),
				delegate);
	}

	public static ServiceInstanceListSupplier discoveryClientBased(
			ReactiveDiscoveryClient discoveryClient, Environment environment) {
		return new DiscoveryClientServiceInstanceListSupplier(discoveryClient,
				environment);
	}

	public static ServiceInstanceListSupplier discoveryClientBased(
			DiscoveryClient discoveryClient, Environment environment) {
		return new DiscoveryClientServiceInstanceListSupplier(discoveryClient,
				environment);
	}

	public static ServiceInstanceListSupplier zonePreferenceBased(
			ServiceInstanceListSupplier delegate, LoadBalancerZoneConfig zoneConfig) {
		return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig);
	}

	public static ServiceInstanceListSupplier healthCheckBased(
			ServiceInstanceListSupplier delegate,
			LoadBalancerProperties.HealthCheck healthCheck, WebClient webClient) {
		return new HealthCheckServiceInstanceListSupplier(delegate, healthCheck,
				webClient);
	}

}
