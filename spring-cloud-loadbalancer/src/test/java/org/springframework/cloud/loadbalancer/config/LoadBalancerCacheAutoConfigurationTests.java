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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadBalancerCacheAutoConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 */

class LoadBalancerCacheAutoConfigurationTests {

	@Test
	void shouldAutoEnableCaching() {
		AnnotationConfigApplicationContext context = setup("");
		assertThat(context.getBeansOfType(CacheManager.class)).isNotEmpty();
		assertThat(context.getBeansOfType(CacheManager.class).get("cacheManager"))
				.isNotInstanceOf(NoOpCacheManager.class);
	}

	@Test
	void shouldUseNoOpCacheIfCacheTypeNone() {
		AnnotationConfigApplicationContext context = setup("spring.cache.type=none");
		assertThat(context.getBeansOfType(CacheManager.class)).isNotEmpty();
		assertThat(context.getBeansOfType(CacheManager.class).get("cacheManager"))
				.isInstanceOf(NoOpCacheManager.class);
	}

	private AnnotationConfigApplicationContext setup(String property) {
		List<Class> config = new ArrayList<>();
		config.add(LoadBalancerCacheAutoConfiguration.class);
		config.add(CacheAutoConfiguration.class);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (StringUtils.hasText(property)) {
			TestPropertyValues.of(property).applyTo(context);
		}
		context.register(config.toArray(new Class[0]));
		context.refresh();
		return context;
	}

}
