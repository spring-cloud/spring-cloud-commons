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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeLazyIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class RefreshScopeLazyIntegrationTests {

	@Autowired
	private Service service;

	@Autowired
	private TestProperties properties;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Before
	public void init() {
		// The RefreshScope is lazy (eager=false) so it won't have been instantiated yet
		assertEquals(0, ExampleService.getInitCount());
		ExampleService.reset();
	}

	@After
	public void close() {
		ExampleService.reset();
	}

	@Test
	@DirtiesContext
	public void testSimpleProperties() throws Exception {
		assertEquals("Hello scope!", this.service.getMessage());
		assertTrue(this.service instanceof Advised);
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...but don't refresh, so the bean stays the same:
		assertEquals("Hello scope!", this.service.getMessage());
		assertEquals(1, ExampleService.getInitCount());
		assertEquals(0, ExampleService.getDestroyCount());
	}

	@Test
	@DirtiesContext
	public void testRefresh() throws Exception {
		assertEquals("Hello scope!", this.service.getMessage());
		String id1 = this.service.toString();
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		String id2 = this.service.toString();
		assertEquals("Foo", this.service.getMessage());
		assertEquals(2, ExampleService.getInitCount());
		assertEquals(1, ExampleService.getDestroyCount());
		assertNotSame(id1, id2);
		assertNotNull(ExampleService.event);
		assertEquals(RefreshScopeRefreshedEvent.DEFAULT_NAME,
				ExampleService.event.getName());
	}

	@Test
	@DirtiesContext
	public void testRefreshBean() throws Exception {
		assertEquals("Hello scope!", this.service.getMessage());
		String id1 = this.service.toString();
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refresh("service");
		String id2 = this.service.toString();
		assertEquals("Foo", this.service.getMessage());
		assertEquals(2, ExampleService.getInitCount());
		assertEquals(1, ExampleService.getDestroyCount());
		assertNotSame(id1, id2);
		assertNotNull(ExampleService.event);
		assertEquals(GenericScope.SCOPED_TARGET_PREFIX + "service",
				ExampleService.event.getName());
	}

	public static interface Service {

		String getMessage();

	}

	public static class ExampleService implements Service, InitializingBean,
			DisposableBean, ApplicationListener<RefreshScopeRefreshedEvent> {

		private static Log logger = LogFactory.getLog(ExampleService.class);

		private volatile static int initCount = 0;
		private volatile static int destroyCount = 0;
		private volatile static RefreshScopeRefreshedEvent event;

		private String message = null;
		private volatile long delay = 0;

		public void setDelay(long delay) {
			this.delay = delay;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			logger.debug("Initializing message: " + this.message);
			initCount++;
		}

		@Override
		public void destroy() throws Exception {
			logger.debug("Destroying message: " + this.message);
			destroyCount++;
			this.message = null;
		}

		public static void reset() {
			initCount = 0;
			destroyCount = 0;
			event = null;
		}

		public static int getInitCount() {
			return initCount;
		}

		public static int getDestroyCount() {
			return destroyCount;
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

		@Override
		public void onApplicationEvent(RefreshScopeRefreshedEvent e) {
			event = e;
		}
	}

	@Configuration
	@EnableConfigurationProperties(TestProperties.class)
	@ImportAutoConfiguration({ RefreshAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Autowired
		private TestProperties properties;

		@Bean
		public static org.springframework.cloud.context.scope.refresh.RefreshScope refreshScope() {
			org.springframework.cloud.context.scope.refresh.RefreshScope scope = new org.springframework.cloud.context.scope.refresh.RefreshScope();
			scope.setEager(false);
			return scope;
		}

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
