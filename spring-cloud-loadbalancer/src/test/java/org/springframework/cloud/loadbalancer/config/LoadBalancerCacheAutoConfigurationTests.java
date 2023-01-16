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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cloud.loadbalancer.cache.DefaultLoadBalancerCacheManager;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadBalancerCacheAutoConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 */

class LoadBalancerCacheAutoConfigurationTests {

	@Test
	void shouldAutoEnableCaching() {
		ApplicationContextRunner contextRunner = baseApplicationRunner();

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
			assertThat(((CacheManager) context.getBean("caffeineLoadBalancerCacheManager")).getCacheNames()).hasSize(1);
			assertThat(context.getBean("caffeineLoadBalancerCacheManager")).isInstanceOf(CaffeineCacheManager.class);
			assertThat(((CacheManager) context.getBean("caffeineLoadBalancerCacheManager")).getCacheNames())
					.contains("CachingServiceInstanceListSupplierCache");
		});
	}

	@Test
	void caffeineLoadBalancerCacheShouldNotOverrideCacheTypeSetting() {
		ApplicationContextRunner contextRunner = baseApplicationRunner().withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.cache.type=none");

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
			assertThat(context.getBean("caffeineLoadBalancerCacheManager")).isInstanceOf(CaffeineCacheManager.class);
			assertThat(context.getBeansOfType(CacheManager.class).get("cacheManager"))
					.isInstanceOf(NoOpCacheManager.class);

		});
	}

	@Test
	void loadBalancerCacheShouldNotOverrideExistingCaffeineCacheManager() {
		ApplicationContextRunner contextRunner = baseApplicationRunner().withUserConfiguration(TestConfiguration.class);

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
			assertThat(context.getBean("cacheManager")).isInstanceOf(CaffeineCacheManager.class);
			assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames()).isEmpty();
			assertThat(((CacheManager) context.getBean("caffeineLoadBalancerCacheManager")).getCacheNames()).hasSize(1);
			assertThat(((CacheManager) context.getBean("caffeineLoadBalancerCacheManager")).getCacheNames())
					.contains("CachingServiceInstanceListSupplierCache");
		});

	}

	@Test
	void shouldNotInstantiateCaffeineLoadBalancerCacheIfDisabled() {
		ApplicationContextRunner contextRunner = baseApplicationRunner()
				.withPropertyValues("spring.cloud.loadbalancer.cache.enabled=false")
				.withUserConfiguration(TestConfiguration.class);

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
			assertThat(context.getBean("cacheManager")).isInstanceOf(CaffeineCacheManager.class);
			assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames()).isEmpty();
		});
	}

	@Test
	void shouldUseDefaultCacheIfCaffeineNotInClasspath() {
		ApplicationContextRunner contextRunner = noCaffeineRunner();

		contextRunner.run(context -> {
			assertThat(context.getBean(LoadBalancerCacheAutoConfiguration.LoadBalancerCaffeineWarnLogger.class))
					.isNotNull();
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
			assertThat(((CacheManager) context.getBean("defaultLoadBalancerCacheManager")).getCacheNames()).hasSize(1);
			assertThat(context.getBean("defaultLoadBalancerCacheManager"))
					.isInstanceOf(DefaultLoadBalancerCacheManager.class);
			assertThat(((CacheManager) context.getBean("defaultLoadBalancerCacheManager")).getCacheNames())
					.contains("CachingServiceInstanceListSupplierCache");
		});
	}

	@Test
	void shouldUseDefaultCacheIfCaffeineCacheManagerNotInClasspath() {
		ApplicationContextRunner contextRunner = noCaffeineCacheManagerRunner();

		contextRunner.run(context -> {
			assertThat(context.getBean(LoadBalancerCacheAutoConfiguration.LoadBalancerCaffeineWarnLogger.class))
					.isNotNull();
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
			assertThat(((CacheManager) context.getBean("defaultLoadBalancerCacheManager")).getCacheNames()).hasSize(1);
			assertThat(context.getBean("defaultLoadBalancerCacheManager"))
					.isInstanceOf(DefaultLoadBalancerCacheManager.class);
			assertThat(((CacheManager) context.getBean("defaultLoadBalancerCacheManager")).getCacheNames())
					.contains("CachingServiceInstanceListSupplierCache");
		});
	}

	@Test
	void defaultLoadBalancerCacheShouldNotOverrideCacheTypeSetting() {
		ApplicationContextRunner contextRunner = noCaffeineRunner().withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.cache.type=none");

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
			assertThat(context.getBean("defaultLoadBalancerCacheManager"))
					.isInstanceOf(DefaultLoadBalancerCacheManager.class);
			assertThat(context.getBeansOfType(CacheManager.class).get("cacheManager"))
					.isInstanceOf(NoOpCacheManager.class);

		});
	}

	@Test
	void defaultLoadBalancerCacheShouldNotOverrideExistingCacheManager() {
		ApplicationContextRunner contextRunner = noCaffeineRunner().withUserConfiguration(TestConfiguration.class);

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
			assertThat(context.getBean("cacheManager")).isInstanceOf(ConcurrentMapCacheManager.class);
			assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames()).isEmpty();
			assertThat(((CacheManager) context.getBean("defaultLoadBalancerCacheManager")).getCacheNames()).hasSize(1);
			assertThat(((CacheManager) context.getBean("defaultLoadBalancerCacheManager")).getCacheNames())
					.contains("CachingServiceInstanceListSupplierCache");
		});

	}

	@Test
	void shouldNotInstantiateDefaultLoadBalancerCacheIfDisabled() {
		ApplicationContextRunner contextRunner = noCaffeineRunner()
				.withPropertyValues("spring.cloud.loadbalancer.cache.enabled=false")
				.withUserConfiguration(TestConfiguration.class);

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
			assertThat(context.getBean("cacheManager")).isInstanceOf(ConcurrentMapCacheManager.class);
			assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames()).isEmpty();
		});
	}

	@Test
	void shouldNotInstantiateDefaultLoadBalancerCacheIfLoadBalancingDisabled() {
		noCaffeineRunner().withPropertyValues("spring.cloud.loadbalancer.enabled=false")
				.withUserConfiguration(TestConfiguration.class).run(context -> {
					assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
					assertThat(context.getBean("cacheManager")).isInstanceOf(ConcurrentMapCacheManager.class);
					assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames()).isEmpty();
				});
	}

	private ApplicationContextRunner baseApplicationRunner() {
		return new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(CacheAutoConfiguration.class, LoadBalancerCacheAutoConfiguration.class));
	}

	private ApplicationContextRunner noCaffeineRunner() {
		return baseApplicationRunner().withClassLoader(new FilteredClassLoader(Caffeine.class));
	}

	private ApplicationContextRunner noCaffeineCacheManagerRunner() {
		return baseApplicationRunner().withClassLoader(new FilteredClassLoader(CaffeineCacheManager.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class TestConfiguration {

	}

}
