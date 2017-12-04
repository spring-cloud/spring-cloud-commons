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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.MoreRefreshScopeIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class MoreRefreshScopeIntegrationTests {

	@Autowired
	private TestService service;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Autowired
	private ConfigurableEnvironment environment;

	@Before
	public void init() {
		assertEquals(1, TestService.getInitCount());
		TestService.reset();
	}

	@After
	public void close() {
		TestService.reset();
	}

	@Test
	@DirtiesContext
	public void testSimpleProperties() throws Exception {
		assertEquals("Hello scope!", this.service.getMessage());
		assertTrue(this.service instanceof Advised);
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...but don't refresh, so the bean stays the same:
		assertEquals("Hello scope!", this.service.getMessage());
		assertEquals(0, TestService.getInitCount());
		assertEquals(0, TestService.getDestroyCount());
	}

	@Test
	@DirtiesContext
	public void testRefresh() throws Exception {
		assertEquals("Hello scope!", this.service.getMessage());
		String id1 = this.service.toString();
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment, Type.MAP, "morerefreshtests");
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		String id2 = this.service.toString();
		String message = this.service.getMessage();
		assertEquals("Foo", message);
		assertEquals(1, TestService.getInitCount());
		assertEquals(1, TestService.getDestroyCount());
		assertNotSame(id1, id2);
	}

	@Test
	@DirtiesContext
	public void testRefreshFails() throws Exception {
		assertEquals("Hello scope!", this.service.getMessage());
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo", "delay:foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		try {
			// If a refresh fails (e.g. a binding error in this case) the application is
			// basically hosed.
			assertEquals("Hello scope!", this.service.getMessage());
			fail("expected BeanCreationException");
		}
		catch (BeanCreationException e) {
		}
		// But we can fix it by fixing the binding error:
		TestPropertyValues.of("delay:0").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		assertEquals("Foo", this.service.getMessage());
	}


	public static class TestService implements InitializingBean, DisposableBean {

		private static Log logger = LogFactory.getLog(TestService.class);

		private volatile static int initCount = 0;

		private volatile static int destroyCount = 0;

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
	@EnableConfigurationProperties
	@EnableAutoConfiguration
	protected static class TestConfiguration {

		@Bean
		@RefreshScope
		protected TestProperties properties() {
			return new TestProperties();
		}

		@Bean
		@RefreshScope
		public TestService service() {
			TestService service = new TestService();
			service.setMessage(properties().getMessage());
			service.setDelay(properties().getDelay());
			return service;
		}

	}

	@ConfigurationProperties
	// @ManagedResource
	protected static class TestProperties {

		private String message;

		private int delay;

		// @ManagedAttribute
		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		// @ManagedAttribute
		public int getDelay() {
			return this.delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}
	}

}
