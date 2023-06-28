/*
 * Copyright 2012-2023 the original author or authors.
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

import reactor.util.retry.RetrySpec;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RetryAwareServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.XForwardedHeadersTransformer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.support.LoadBalancerEnvironmentPropertyUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author BaoLin Zhu
 * @author changjin wei(魏昌进)
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
public class LoadBalancerClientConfiguration {

	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;

	@Bean
	@ConditionalOnMissingBean
	public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		return new RoundRobinLoadBalancer(
				loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReactiveDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
	public static class ReactiveSupportConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(DefaultConfigurationCondition.class)
		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching().build(context);
		}

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(ZonePreferenceConfigurationCondition.class)
		public ServiceInstanceListSupplier zonePreferenceDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching().withZonePreference()
					.build(context);
		}

		@Bean
		@ConditionalOnBean(LoadBalancerClientFactory.class)
		@ConditionalOnMissingBean
		public XForwardedHeadersTransformer xForwarderHeadersTransformer(LoadBalancerClientFactory clientFactory) {
			return new XForwardedHeadersTransformer(clientFactory);
		}

		@Bean
		@ConditionalOnBean({ ReactiveDiscoveryClient.class, WebClient.Builder.class })
		@ConditionalOnMissingBean
		@Conditional(HealthCheckConfigurationCondition.class)
		public ServiceInstanceListSupplier healthCheckDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withHealthChecks().build(context);
		}

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(RequestBasedStickySessionConfigurationCondition.class)
		public ServiceInstanceListSupplier requestBasedStickySessionDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching()
					.withRequestBasedStickySession().build(context);
		}

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(SameInstancePreferenceConfigurationCondition.class)
		public ServiceInstanceListSupplier sameInstancePreferenceServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching()
					.withSameInstancePreference().build(context);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBlockingDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
	public static class BlockingSupportConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(DefaultConfigurationCondition.class)
		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching().build(context);
		}

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(ZonePreferenceConfigurationCondition.class)
		public ServiceInstanceListSupplier zonePreferenceDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching()
					.withZonePreference().build(context);
		}

		@Bean
		@ConditionalOnBean({ DiscoveryClient.class, RestTemplate.class })
		@ConditionalOnMissingBean
		@Conditional(HealthCheckConfigurationCondition.class)
		public ServiceInstanceListSupplier healthCheckDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withBlockingHealthChecks()
					.build(context);
		}

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(RequestBasedStickySessionConfigurationCondition.class)
		public ServiceInstanceListSupplier requestBasedStickySessionDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching()
					.withRequestBasedStickySession().build(context);
		}

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		@Conditional(SameInstancePreferenceConfigurationCondition.class)
		public ServiceInstanceListSupplier sameInstancePreferenceServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching()
					.withSameInstancePreference().build(context);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBlockingDiscoveryEnabled
	@ConditionalOnClass(RetryTemplate.class)
	@Conditional(BlockingOnAvoidPreviousInstanceAndRetryEnabledCondition.class)
	@AutoConfigureAfter(BlockingSupportConfiguration.class)
	@ConditionalOnBean(ServiceInstanceListSupplier.class)
	public static class BlockingRetryConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@Primary
		public ServiceInstanceListSupplier retryAwareDiscoveryClientServiceInstanceListSupplier(
				ServiceInstanceListSupplier delegate) {
			return new RetryAwareServiceInstanceListSupplier(delegate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReactiveDiscoveryEnabled
	@Conditional(ReactiveOnAvoidPreviousInstanceAndRetryEnabledCondition.class)
	@AutoConfigureAfter(ReactiveSupportConfiguration.class)
	@ConditionalOnBean(ServiceInstanceListSupplier.class)
	@ConditionalOnClass(RetrySpec.class)
	public static class ReactiveRetryConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@Primary
		public ServiceInstanceListSupplier retryAwareDiscoveryClientServiceInstanceListSupplier(
				ServiceInstanceListSupplier delegate) {
			return new RetryAwareServiceInstanceListSupplier(delegate);
		}

	}

	static final class BlockingOnAvoidPreviousInstanceAndRetryEnabledCondition extends AllNestedConditions {

		private BlockingOnAvoidPreviousInstanceAndRetryEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true",
				matchIfMissing = true)
		static class LoadBalancerRetryEnabled {

		}

		@Conditional(AvoidPreviousInstanceEnabledCondition.class)
		static class AvoidPreviousInstanceEnabled {

		}

	}

	static final class ReactiveOnAvoidPreviousInstanceAndRetryEnabledCondition extends AllNestedConditions {

		private ReactiveOnAvoidPreviousInstanceAndRetryEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true")
		static class LoadBalancerRetryEnabled {

		}

		@Conditional(AvoidPreviousInstanceEnabledCondition.class)
		static class AvoidPreviousInstanceEnabled {

		}

	}

	static class AvoidPreviousInstanceEnabledCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return LoadBalancerEnvironmentPropertyUtils.trueOrMissingForClientOrDefault(context.getEnvironment(),
					"retry.avoid-previous-instance");
		}

	}

	static class DefaultConfigurationCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return LoadBalancerEnvironmentPropertyUtils.equalToOrMissingForClientOrDefault(context.getEnvironment(),
					"configurations", "default");
		}

	}

	static class ZonePreferenceConfigurationCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
					"configurations", "zone-preference");
		}

	}

	static class HealthCheckConfigurationCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
					"configurations", "health-check");
		}

	}

	static class RequestBasedStickySessionConfigurationCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
					"configurations", "request-based-sticky-session");
		}

	}

	static class SameInstancePreferenceConfigurationCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
					"configurations", "same-instance-preference");
		}

	}

}
