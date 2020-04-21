package org.springframework.cloud.loadbalancer.annotation.configutil;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
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
 * Utility class containing helper methods for setting up {@link ServiceInstanceListSupplier} configuration
 * in a more concise way.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.3
 */
public class ServiceInstanceListSuppliers {

	public static ServiceInstanceListSupplier cachedOrDelegate(ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider,
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

	public static ServiceInstanceListSupplier discoveryClientBased(ReactiveDiscoveryClient discoveryClient,
			Environment environment) {
		return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, environment);
	}

	public static ServiceInstanceListSupplier discoveryClientBased(DiscoveryClient discoveryClient,
			Environment environment) {
		return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, environment);
	}

	public static ServiceInstanceListSupplier zonePreferenceBased(ServiceInstanceListSupplier delegate,
			LoadBalancerZoneConfig zoneConfig) {
		return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig);
	}

	public static ServiceInstanceListSupplier healthCheckBased(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties.HealthCheck healthCheck, WebClient webClient) {
		return new HealthCheckServiceInstanceListSupplier(delegate, healthCheck, webClient);
	}
}
