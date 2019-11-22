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

package org.springframework.cloud.loadbalancer.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
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
			assertThat(((CacheManager) context.getBean("loadBalancerCacheManager"))
					.getCacheNames()).hasSize(1);
			assertThat(context.getBean("loadBalancerCacheManager"))
					.isInstanceOf(CaffeineCacheManager.class);
			assertThat(((CacheManager) context.getBean("loadBalancerCacheManager"))
					.getCacheNames()).contains("CachingServiceInstanceListSupplierCache");
		});
	}

	@Test
	void loadBalancerCacheShouldNotOverrideCacheTypeSetting() {
		ApplicationContextRunner contextRunner = baseApplicationRunner()
				.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.cache.type=none");

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
			assertThat(context.getBean("loadBalancerCacheManager"))
					.isInstanceOf(CaffeineCacheManager.class);
			assertThat(context.getBeansOfType(CacheManager.class).get("cacheManager"))
					.isInstanceOf(NoOpCacheManager.class);

		});
	}

	@Test
	void loadBalancerCacheShouldNotOverrideExistingCaffeineCacheManager() {
		ApplicationContextRunner contextRunner = baseApplicationRunner()
				.withUserConfiguration(TestConfiguration.class);

		contextRunner.run(context -> {
			assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
			assertThat(context.getBean("cacheManager"))
					.isInstanceOf(CaffeineCacheManager.class);
			assertThat(((CacheManager) context.getBean("cacheManager")).getCacheNames())
					.isEmpty();
			assertThat(((CacheManager) context.getBean("loadBalancerCacheManager"))
					.getCacheNames()).hasSize(1);
			assertThat(((CacheManager) context.getBean("loadBalancerCacheManager"))
					.getCacheNames()).contains("CachingServiceInstanceListSupplierCache");
		});

	}

	private ApplicationContextRunner baseApplicationRunner() {
		return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
				CacheAutoConfiguration.class, LoadBalancerCacheAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class TestConfiguration {

	}

}
