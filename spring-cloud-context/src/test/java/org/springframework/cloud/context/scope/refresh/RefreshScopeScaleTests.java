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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeScaleTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class RefreshScopeScaleTests {

	private static Log logger = LogFactory.getLog(RefreshScopeScaleTests.class);

	@Autowired
	org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	private ExecutorService executor = Executors.newFixedThreadPool(8);

	@Autowired
	private ExampleService service;

	@Autowired
	private TestProperties properties;

	@Test
	@Repeat(10)
	@DirtiesContext
	public void testConcurrentRefresh() throws Exception {

		// overload the thread pool and try to force Spring to create too many instances
		int n = 80;
		ExampleService.count = 0;
		this.properties.setMessage("Foo");
		this.properties.setDelay(500);
		this.scope.refreshAll();
		final CountDownLatch latch = new CountDownLatch(n);
		Future<String> result = null;
		for (int i = 0; i < n; i++) {
			result = this.executor.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					logger.debug("Background started.");
					try {
						return RefreshScopeScaleTests.this.service.getMessage();
					}
					finally {
						latch.countDown();
						logger.debug("Background done.");
					}
				}
			});
		}
		then(latch.await(15000, TimeUnit.MILLISECONDS)).isTrue();
		then(this.service.getMessage()).isEqualTo("Foo");
		then(result.get()).isNotNull();
		then(result.get()).isEqualTo("Foo");
		then(ExampleService.count).isEqualTo(1);
	}

	public interface Service {

		String getMessage();

	}

	public static class ExampleService
			implements Service, InitializingBean, DisposableBean {

		private static Log logger = LogFactory.getLog(ExampleService.class);

		private static volatile int count;

		private String message = null;

		private volatile long delay = 0;

		public void setDelay(long delay) {
			this.delay = delay;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			ExampleService.count++;
			try {
				Thread.sleep(this.delay);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			logger.debug("Initializing message: " + this.message);
		}

		@Override
		public void destroy() throws Exception {
			logger.debug("Destroying message: " + this.message);
			this.message = null;
		}

		@Override
		public String getMessage() {
			logger.debug("Returning message: " + this.message);
			return this.message;
		}

		public void setMessage(String message) {
			logger.debug("Setting message: " + message);
			this.message = message;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(TestProperties.class)
	@Import({ RefreshAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Autowired
		private TestProperties properties;

		@Bean
		@RefreshScope
		public ExampleService service() {
			ExampleService service = new ExampleService();
			service.setMessage(this.properties.getMessage());
			service.setDelay(this.properties.getDelay());
			return service;
		}

	}

	@ConfigurationProperties
	@ManagedResource
	protected static class TestProperties {

		private String message;

		private int delay;

		@ManagedAttribute
		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@ManagedAttribute
		public int getDelay() {
			return this.delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

	}

}
