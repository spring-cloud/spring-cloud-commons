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

package org.springframework.cloud.loadbalancer.config;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cloud.loadbalancer.cache.CaffeineBasedLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.DefaultLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * An AutoConfiguration that automatically enables caching when Spring Boot, and Spring
 * Framework Cache support are present. If Caffeine is present in the classpath, it will
 * be used for loadbalancer caching. If not, a default cache will be used.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 * @see CacheManager
 * @see CacheAutoConfiguration
 * @see CacheAspectSupport
 * @see <a href="https://github.com/ben-manes/caffeine>Caffeine</a>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ CacheManager.class, CacheAutoConfiguration.class })
@AutoConfigureAfter(CacheAutoConfiguration.class)
@EnableConfigurationProperties(LoadBalancerCacheProperties.class)
@Conditional(LoadBalancerCacheAutoConfiguration.OnLoadBalancerCachingEnabledCondition.class)
public class LoadBalancerCacheAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnCaffeineCacheMissingCondition.class)
	protected static class LoadBalancerCacheManagerWarnConfiguration {

		@Bean
		LoadBalancerCaffeineWarnLogger caffeineWarnLogger() {
			return new LoadBalancerCaffeineWarnLogger();
		}

	}

	static class LoadBalancerCaffeineWarnLogger implements InitializingBean {

		private static final Log LOG = LogFactory.getLog(LoadBalancerCaffeineWarnLogger.class);

		@Override
		public void afterPropertiesSet() {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Spring Cloud LoadBalancer is currently working with the default cache. "
						+ "While this cache implementation is useful for development and tests, it's recommended to use Caffeine cache in production."
						+ "You can switch to using Caffeine cache, by adding it and org.springframework.cache.caffeine.CaffeineCacheManager to the classpath.");
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Caffeine.class, CaffeineCacheManager.class })
	protected static class CaffeineLoadBalancerCacheManagerConfiguration {

		@Bean(autowireCandidate = false)
		@ConditionalOnMissingBean
		LoadBalancerCacheManager caffeineLoadBalancerCacheManager(LoadBalancerCacheProperties cacheProperties) {
			return new CaffeineBasedLoadBalancerCacheManager(cacheProperties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnCaffeineCacheMissingCondition.class)
	@ConditionalOnClass(ConcurrentMapWithTimedEviction.class)
	protected static class DefaultLoadBalancerCacheManagerConfiguration {

		@Bean(autowireCandidate = false)
		@ConditionalOnMissingBean
		LoadBalancerCacheManager defaultLoadBalancerCacheManager(LoadBalancerCacheProperties cacheProperties) {
			return new DefaultLoadBalancerCacheManager(cacheProperties);
		}

	}

	static final class OnCaffeineCacheMissingCondition extends AnyNestedCondition {

		private OnCaffeineCacheMissingCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingClass("com.github.benmanes.caffeine.cache.Caffeine")
		static class CaffeineClassMissing {

		}

		@ConditionalOnMissingClass("org.springframework.cache.caffeine.CaffeineCacheManager")
		static class CaffeineCacheManagerClassMissing {

		}

	}

	static final class OnLoadBalancerCachingEnabledCondition extends AllNestedConditions {

		OnLoadBalancerCachingEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
		static class LoadBalancerEnabled {

		}

		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.cache.enabled", matchIfMissing = true)
		static class LoadBalancerCacheEnabled {

		}

	}

}

// Remove after adding hints to GraalVM reachability metadata repo
class CaffeineHints implements RuntimeHintsRegistrar {

	private static final Log LOG = LogFactory.getLog(CaffeineHints.class);

	private static final String CAFFEINE_BOUNDED_LOCAL_CACHE_CLASS_NAME = "com.github.benmanes.caffeine.cache.BoundedLocalCache";

	private static final String CAFFEINE_CACHE_BASE_PACKAGE = "com/github/benmanes/caffeine/cache";

	private static final String CAFFEINE_NODE_CLASS_NAME = "com.github.benmanes.caffeine.cache.Node";

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (!ClassUtils.isPresent("com.github.benmanes.caffeine.cache.Caffeine", classLoader)) {
			return;
		}
		hints.reflection()
				.registerType(TypeReference.of(Caffeine.class),
						hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS,
								MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
				.registerType(TypeReference.of("com.github.benmanes.caffeine.cache.BoundedLocalCache"),
						hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS,
								MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
				.registerType(TypeReference.of("com.github.benmanes.caffeine.cache.LocalCacheFactory"),
						hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS,
								MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
				.registerType(TypeReference.of("com.github.benmanes.caffeine.cache.Node"),
						hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS,
								MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS));
		getCaffeineSubtypes().forEach(cacheType -> hints.reflection().registerType(TypeReference.of(cacheType),
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS)));
	}

	private Set<String> getCaffeineSubtypes() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		try {
			provider.addIncludeFilter(new AssignableTypeFilter(Class.forName(CAFFEINE_BOUNDED_LOCAL_CACHE_CLASS_NAME)));
			provider.addIncludeFilter(new AssignableTypeFilter(Class.forName(CAFFEINE_NODE_CLASS_NAME)));
		}
		catch (ClassNotFoundException e) {
			LOG.warn("Could not get class for name: " + CAFFEINE_BOUNDED_LOCAL_CACHE_CLASS_NAME);
		}
		return provider.findCandidateComponents(CAFFEINE_CACHE_BASE_PACKAGE).stream().filter(Objects::nonNull)
				.map(BeanDefinition::getBeanClassName).filter(Objects::nonNull).collect(Collectors.toSet());
	}

}
