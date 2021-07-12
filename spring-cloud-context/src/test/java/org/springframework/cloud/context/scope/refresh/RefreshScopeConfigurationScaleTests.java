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

package org.springframework.cloud.context.scope.refresh;

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
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationScaleTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class, properties = {
		"logging.level.org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationScaleTests=DEBUG" })
public class RefreshScopeConfigurationScaleTests {

	private static Log logger = LogFactory.getLog(RefreshScopeConfigurationScaleTests.class);

	@Autowired
	org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	private ExecutorService executor = Executors.newFixedThreadPool(8);

	@Autowired
	private ExampleService service;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@Repeat(10)
	@DirtiesContext
	public void testConcurrentRefresh() throws Exception {

		this.scope.setEager(false);

		// overload the thread pool and try to force Spring to create too many instances
		int n = 80;
		TestPropertyValues.of("message=Foo").applyTo(this.environment);
		this.scope.refreshAll();
		final CountDownLatch latch = new CountDownLatch(n);
		List<Future<String>> results = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			results.add(this.executor.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					logger.debug("Background started.");
					try {
						return RefreshScopeConfigurationScaleTests.this.service.getMessage();
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
					RefreshScopeConfigurationScaleTests.this.scope.refreshAll();
				}
			});
		}
		then(latch.await(15000, TimeUnit.MILLISECONDS)).isTrue();
		then(this.service.getMessage()).isEqualTo("Foo");
		for (Future<String> result : results) {
			then(result.get()).isEqualTo("Foo");
		}
	}

	public interface Service {

		String getMessage();

	}

	public static class ExampleService implements Service, InitializingBean, DisposableBean {

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

		@Override
		public String getMessage() {
			logger.debug("Returning message: " + ObjectUtils.getIdentityHexString(this) + ", " + this.message);
			return this.message;
		}

		public void setMessage(String message) {
			logger.debug("Setting message: " + ObjectUtils.getIdentityHexString(this) + ", " + message);
			this.message = message;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		@RefreshScope
		public TestProperties properties() {
			return new TestProperties();
		}

		@Bean
		@RefreshScope
		public ExampleService service(TestProperties properties) {
			ExampleService service = new ExampleService();
			service.setMessage(properties.getMessage());
			service.setDelay(properties.getDelay());
			return service;
		}

	}

	@ConfigurationProperties
	protected static class TestProperties {

		private String message;

		private int delay;

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getDelay() {
			return this.delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

	}

}
