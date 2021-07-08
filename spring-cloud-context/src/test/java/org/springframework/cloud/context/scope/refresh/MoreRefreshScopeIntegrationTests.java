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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class)
public class MoreRefreshScopeIntegrationTests {

	@Autowired
	private TestService service;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Autowired
	private ConfigurableEnvironment environment;

	@BeforeEach
	public void init() {
		then(TestService.getInitCount()).isEqualTo(1);
		TestService.reset();
	}

	@AfterEach
	public void close() {
		TestService.reset();
	}

	@Test
	@DirtiesContext
	public void testSimpleProperties() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		then(this.service instanceof Advised).isTrue();
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment);
		// ...but don't refresh, so the bean stays the same:
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		then(TestService.getInitCount()).isEqualTo(0);
		then(TestService.getDestroyCount()).isEqualTo(0);
	}

	@Test
	@DirtiesContext
	public void testRefresh() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		String id1 = this.service.toString();
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo").applyTo(this.environment, Type.MAP, "morerefreshtests");
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		String id2 = this.service.toString();
		String message = this.service.getMessage();
		then(message).isEqualTo("Foo");
		then(TestService.getInitCount()).isEqualTo(1);
		then(TestService.getDestroyCount()).isEqualTo(1);
		then(id2).isNotSameAs(id1);
	}

	@Test
	@DirtiesContext
	public void testRefreshFails() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		// Change the dynamic property source...
		TestPropertyValues.of("message:Foo", "delay:foo").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		try {
			// If a refresh fails (e.g. a binding error in this case) the application is
			// basically hosed.
			then(this.service.getMessage()).isEqualTo("Hello scope!");
			fail("expected BeanCreationException");
		}
		catch (BeanCreationException e) {
		}
		// But we can fix it by fixing the binding error:
		TestPropertyValues.of("delay:0").applyTo(this.environment);
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		then(this.service.getMessage()).isEqualTo("Foo");
	}

	public static class TestService implements InitializingBean, DisposableBean {

		private static Log logger = LogFactory.getLog(TestService.class);

		private volatile static int initCount = 0;

		private volatile static int destroyCount = 0;

		private String message = null;

		private volatile long delay = 0;

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

		public void setMessage(String message) {
			logger.debug("Setting message: " + message);
			this.message = message;
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
