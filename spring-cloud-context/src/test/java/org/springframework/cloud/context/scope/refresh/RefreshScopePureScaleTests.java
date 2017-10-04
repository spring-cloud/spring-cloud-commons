/*
 * Copyright 2006-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.context.scope.refresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopePureScaleTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
// , properties="logging.level.org.springframework.cloud.context.scope.refresh.RefreshScopePureScaleTests=DEBUG")
public class RefreshScopePureScaleTests {

	private static Log logger = LogFactory.getLog(RefreshScopePureScaleTests.class);

	private ExecutorService executor = Executors.newFixedThreadPool(8);

	@Autowired
	org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Autowired
	private ExampleService service;

	@Test
	@Repeat(10)
	@DirtiesContext
	public void testConcurrentRefresh() throws Exception {

		// overload the thread pool and try to force Spring to create too many instances
		int n = 80;
		this.scope.refreshAll();
		final CountDownLatch latch = new CountDownLatch(n);
		List<Future<String>> results = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			results.add(this.executor.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					logger.debug("Background started.");
					try {
						return RefreshScopePureScaleTests.this.service.getMessage();
					}
					finally {
						latch.countDown();
						logger.debug("Background done.");
					}
				}
			}));
			this.executor.submit(new Runnable() {
				@Override
				public void run() {
					logger.debug("Refreshing.");
					RefreshScopePureScaleTests.this.scope.refreshAll();
				}});
		}
		assertTrue(latch.await(15000, TimeUnit.MILLISECONDS));
		assertEquals("Foo", this.service.getMessage());
		for (Future<String> result : results) {
			assertEquals("Foo", result.get());
		}
	}

	public static interface Service {

		String getMessage();

	}

	public static class ExampleService
			implements Service, InitializingBean, DisposableBean {

		private static Log logger = LogFactory.getLog(ExampleService.class);

		private String message = null;
		private volatile long delay = 0;

		public void setDelay(long delay) {
			this.delay = delay;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			logger.debug("Initializing: " + ObjectUtils.getIdentityHexString(this) + ", " + this.message);
			try {
				Thread.sleep(this.delay);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			logger.debug("Initialized: " + ObjectUtils.getIdentityHexString(this) + ", " + this.message);
		}

		@Override
		public void destroy() throws Exception {
			logger.debug("Destroying message: " + ObjectUtils.getIdentityHexString(this) + ", " + this.message);
			this.message = null;
		}

		public void setMessage(String message) {
			logger.debug("Setting message: " + ObjectUtils.getIdentityHexString(this) + ", " + message);
			this.message = message;
		}

		@Override
		public String getMessage() {
			logger.debug("Returning message: " + ObjectUtils.getIdentityHexString(this) + ", " + this.message);
			return this.message;
		}

	}

	@Configuration
	@Import({ RefreshAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		@RefreshScope
		public ExampleService service() {
			ExampleService service = new ExampleService();
			service.setMessage("Foo");
			return service;
		}

	}

}
