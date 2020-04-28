/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.cloud.loadbalancer.annotation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.filter.ServerInstanceFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author Hash.Jang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
public class LoadBalancerClientConfiguration {

	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;

	@Bean
	@ConditionalOnMissingBean
	public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
			Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

		List<ServerInstanceFilter> serverInstanceFilters = new ArrayList<>();
		String property = environment
				.getProperty("spring.cloud." + LoadBalancerClientFactory.NAMESPACE + "." + name + ".filters");
		if (StringUtils.hasText(property)) {
			Arrays.stream(property.split(",")).map(String::trim).forEach(className -> {
				if (StringUtils.hasText(className)) {
					try {
						Class<?> toInstantiate = Class.forName(className);
						Constructor constructor = toInstantiate.getConstructor(Environment.class);
						serverInstanceFilters.add((ServerInstanceFilter) constructor.newInstance(environment));
					} catch (Exception e) {
						throw new IllegalArgumentException("failed to load " + className + " named " + name, e);
					}
				}
			});
		}
		return new RoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider(name,
				ServiceInstanceListSupplier.class), serverInstanceFilters, name);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReactiveDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
	public static class ReactiveSupportConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
				ReactiveDiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			DiscoveryClientServiceInstanceListSupplier delegate = new DiscoveryClientServiceInstanceListSupplier(
					discoveryClient, env);
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceListSupplier(delegate,
						cacheManagerProvider.getIfAvailable());
			}
			return delegate;
		}

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceSupplier discoveryClientServiceInstanceSupplier(
				ReactiveDiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			DiscoveryClientServiceInstanceSupplier delegate = new DiscoveryClientServiceInstanceSupplier(
					discoveryClient, env);
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceSupplier(delegate,
						cacheManagerProvider.getIfAvailable());
			}
			return delegate;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBlockingDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
	public static class BlockingSupportConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
				DiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			DiscoveryClientServiceInstanceListSupplier delegate = new DiscoveryClientServiceInstanceListSupplier(
					discoveryClient, env);
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceListSupplier(delegate,
						cacheManagerProvider.getIfAvailable());
			}
			return delegate;
		}

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceSupplier discoveryClientServiceInstanceSupplier(
				DiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			DiscoveryClientServiceInstanceSupplier delegate = new DiscoveryClientServiceInstanceSupplier(
					discoveryClient, env);
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceSupplier(delegate,
						cacheManagerProvider.getIfAvailable());
			}
			return delegate;
		}

	}

}
