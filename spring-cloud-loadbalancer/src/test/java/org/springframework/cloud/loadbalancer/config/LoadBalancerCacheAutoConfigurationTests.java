/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cloud.loadbalancer.cache.DefaultLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.RedisLoadBalancerCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
			assertThat(context.getBean("caffeineLoadBalancerCacheManager")).isInstanceOf(CaffeineCacheManager.class);
			assertThat(((CacheManager) context.getBean("caffeineLoadBalancerCacheManager")).getCacheNames())
				.contains("CachingServiceInstanceListSupplierCache");
		});
	}

	@Test
	void shouldUseRedisCacheManagerForLoadBalancerCaching() {
		ApplicationContextRunner contextRunner = baseApplicationRunner()
			.withUserConfiguration(RedisTestConfiguration.class)
			.withPropertyValues("spring.cache.type=redis");

		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean("redisLoadBalancerCacheManager");
			assertThat(context.getBean("redisLoadBalancerCacheManager"))
				.isInstanceOf(RedisLoadBalancerCacheManager.class);
			assertThat(context.getBeansOfType(LoadBalancerCacheManager.class)).hasSize(1);
		});
	}

	@Test
	void shouldUseRedisCacheManagerWhenNoApplicationCacheManagerExists() {
		baseApplicationRunner().withUserConfiguration(RedisConnectionFactoryOnlyConfiguration.class)
			.withPropertyValues("spring.cache.type=redis")
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("redisLoadBalancerCacheManager");
				assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
				assertThat(context.getBeansOfType(LoadBalancerCacheManager.class)).hasSize(1);
			});
	}

	@Test
	void shouldUseRedisCacheManagerWhenMultipleApplicationCacheManagersExist() {
		baseApplicationRunner().withUserConfiguration(RedisTestConfigurationWithMultipleCacheManagers.class)
			.withPropertyValues("spring.cache.type=redis")
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("redisLoadBalancerCacheManager");
				assertThat(context.getBeansOfType(CacheManager.class)).hasSize(3);
				assertThat(context.getBeansOfType(LoadBalancerCacheManager.class)).hasSize(1);
			});
	}

	@Test
	void shouldUseCaffeineWhenRedisIsSelectedButConnectionFactoryIsMissing() {
		ApplicationContextRunner contextRunner = baseApplicationRunner().withPropertyValues("spring.cache.type=redis");

		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean("redisLoadBalancerCacheManager");
			assertThat(context).hasBean("caffeineLoadBalancerCacheManager");
			assertThat(context.getBean("caffeineLoadBalancerCacheManager")).isInstanceOf(CaffeineCacheManager.class);
			assertThat(context.getBeansOfType(LoadBalancerCacheManager.class)).hasSize(1);
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
			.withUserConfiguration(TestConfiguration.class)
			.run(context -> {
				assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
				assertThat(context.getBean("cacheManager")).isInstanceOf(ConcurrentMapCacheManager.class);
				assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames()).isEmpty();
			});
	}

	@Test
	void shouldUseDefaultCacheWhenRedisIsSelectedButConnectionFactoryIsMissingAndCaffeineIsUnavailable() {
		noCaffeineRunner().withPropertyValues("spring.cache.type=redis").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean("redisLoadBalancerCacheManager");
			assertThat(context).hasBean("defaultLoadBalancerCacheManager");
			assertThat(context.getBean("defaultLoadBalancerCacheManager"))
				.isInstanceOf(DefaultLoadBalancerCacheManager.class);
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

	@Configuration(proxyBeanMethods = false)
	static class RedisConnectionFactoryOnlyConfiguration {

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RedisTestConfiguration {

		@Bean
		@Primary
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RedisTestConfigurationWithMultipleCacheManagers {

		@Bean
		CacheManager firstCacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		CacheManager secondCacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

	}

}
