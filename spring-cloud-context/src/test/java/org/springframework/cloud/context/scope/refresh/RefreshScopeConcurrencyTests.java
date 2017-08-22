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
import org.springframework.cloud.context.scope.refresh.RefreshScopeConcurrencyTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class RefreshScopeConcurrencyTests {

	private static Log logger = LogFactory.getLog(RefreshScopeConcurrencyTests.class);

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	@Autowired
	private Service service;

	@Autowired
	private TestProperties properties;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Test
	@Repeat(10)
	@DirtiesContext
	public void testConcurrentRefresh() throws Exception {

		assertEquals("Hello scope!", this.service.getMessage());
		this.properties.setMessage("Foo");
		this.properties.setDelay(500);
		final CountDownLatch latch = new CountDownLatch(1);
		Future<String> result = this.executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				logger.debug("Background started.");
				try {
					return RefreshScopeConcurrencyTests.this.service.getMessage();
				}
				finally {
					latch.countDown();
					logger.debug("Background done.");
				}
			}
		});
		assertTrue(latch.await(1500, TimeUnit.MILLISECONDS));
		logger.info("Refreshing");
		this.scope.refreshAll();
		assertEquals("Foo", this.service.getMessage());
		/*
		 * This is the most important assertion: we don't want a null value because that
		 * means the bean was destroyed and not re-initialized before we accessed it.
		 */
		assertNotNull(result.get());
		assertEquals("Hello scope!", result.get());
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
			logger.debug("Initializing message: " + this.message);
		}

		@Override
		public void destroy() throws Exception {
			logger.debug("Destroying message: " + this.message);
			this.message = null;
		}

		public void setMessage(String message) {
			logger.debug("Setting message: " + message);
			this.message = message;
		}

		@Override
		public String getMessage() {
			logger.debug("Getting message: " + this.message);
			try {
				Thread.sleep(this.delay);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			logger.info("Returning message: " + this.message);
			return this.message;
		}

	}

	@Configuration
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
