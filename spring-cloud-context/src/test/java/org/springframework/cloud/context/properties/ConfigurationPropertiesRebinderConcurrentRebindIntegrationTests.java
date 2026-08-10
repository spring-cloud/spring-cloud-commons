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

package org.springframework.cloud.context.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderConcurrentRebindIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that concurrent {@link ConfigurationPropertiesRebinder#rebind(String)} calls
 * for the <em>same</em> bean are serialized, so their destroy/reset/re-initialize steps
 * cannot interleave on the live bean. This is a narrower, safely-fixable slice of the
 * concurrency concerns raised in gh-1709: it does not make the bean safe to read
 * concurrently while a rebind is in progress (that would require either proxying every
 * {@code @ConfigurationProperties} bean, as
 * {@link org.springframework.cloud.context.config.annotation.RefreshScope} already does,
 * or a breaking change to how such properties are consumed), but it does close a real,
 * previously entirely-unguarded hole: two threads rebinding the same bean at once (for
 * example a manual refresh racing with a config-watch-triggered one).
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class)
public class ConfigurationPropertiesRebinderConcurrentRebindIntegrationTests {

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Test
	@DirtiesContext
	public void concurrentRebindsOfSameBeanDoNotInterleave() throws Exception {
		TestProperties.active.set(0);
		TestProperties.maxActive.set(0);
		int threadCount = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < threadCount; i++) {
				futures.add(pool.submit(() -> this.rebinder.rebind("testProperties")));
			}
			for (Future<?> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdown();
		}
		// If rebinds of the same bean were allowed to interleave, more than one thread
		// would be inside the destroy/re-initialize window at the same time.
		then(TestProperties.maxActive.get()).isEqualTo(1);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties();
		}

	}

	// Hack out a protected inner class for testing
	protected static class RefreshConfiguration extends RefreshAutoConfiguration {

		@Configuration(proxyBeanMethods = false)
		protected static class RebinderConfiguration extends ConfigurationPropertiesRebinderAutoConfiguration {

			public RebinderConfiguration(ApplicationContext context) {
				super(context);
			}

		}

	}

	@ConfigurationProperties("test")
	protected static class TestProperties implements InitializingBean {

		private static final AtomicInteger active = new AtomicInteger();

		private static final AtomicInteger maxActive = new AtomicInteger();

		private String message = "initial";

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			int current = this.active.incrementAndGet();
			this.maxActive.accumulateAndGet(current, Math::max);
			try {
				// Widen the window so overlapping, unserialized rebinds would reliably
				// collide here rather than depending on unlucky scheduling.
				Thread.sleep(20);
			}
			finally {
				this.active.decrementAndGet();
			}
		}

	}

}
