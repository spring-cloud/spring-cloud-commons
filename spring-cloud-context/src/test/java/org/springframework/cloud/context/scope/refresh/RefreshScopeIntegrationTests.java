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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class)
class RefreshScopeIntegrationTests {

	@Autowired
	private Service service;

	@Autowired
	private TestProperties properties;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@BeforeEach
	void init() {
		then(ExampleService.getInitCount()).isEqualTo(1);
		ExampleService.reset();
	}

	@AfterEach
	void close() {
		ExampleService.reset();
	}

	@Test
	@DirtiesContext
	void testSimpleProperties() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		then(this.service instanceof Advised).isTrue();
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...but don't refresh, so the bean stays the same:
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		then(ExampleService.getInitCount()).isEqualTo(0);
		then(ExampleService.getDestroyCount()).isEqualTo(0);
	}

	@Test
	@DirtiesContext
	void testRefresh() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		String id1 = this.service.toString();
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refreshAll();
		String id2 = this.service.toString();
		then(this.service.getMessage()).isEqualTo("Foo");
		then(ExampleService.getInitCount()).isEqualTo(1);
		then(ExampleService.getDestroyCount()).isEqualTo(1);
		then(id2).isNotSameAs(id1);
		then(ExampleService.event).isNotNull();
		then(ExampleService.event.getName()).isEqualTo(RefreshScopeRefreshedEvent.DEFAULT_NAME);
	}

	@Test
	@DirtiesContext
	void testRefreshBean() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		String id1 = this.service.toString();
		// Change the dynamic property source...
		this.properties.setMessage("Foo");
		// ...and then refresh, so the bean is re-initialized:
		this.scope.refresh("service");
		String id2 = this.service.toString();
		then(this.service.getMessage()).isEqualTo("Foo");
		then(this.service.getMessage()).isEqualTo("Foo");
		then(ExampleService.getInitCount()).isEqualTo(1);
		then(ExampleService.getDestroyCount()).isEqualTo(1);
		then(id2).isNotSameAs(id1);
		then(ExampleService.event).isNotNull();
		then(ExampleService.event.getName()).isEqualTo(ScopedProxyUtils.getTargetBeanName("service"));
	}

	// see gh-349
	@Test
	@DirtiesContext
	void testCheckedException() {
		Assertions.assertThrows(ServiceException.class, () -> {
			this.service.throwsException();
		});
	}

	interface Service {

		String getMessage();

		String throwsException() throws ServiceException;

	}

	static class ExampleService
			implements Service, InitializingBean, DisposableBean, ApplicationListener<RefreshScopeRefreshedEvent> {

		private static Log logger = LogFactory.getLog(ExampleService.class);

		private volatile static int initCount = 0;

		private volatile static int destroyCount = 0;

		private volatile static RefreshScopeRefreshedEvent event;

		private String message = null;

		private volatile long delay = 0;

		static void reset() {
			initCount = 0;
			destroyCount = 0;
			event = null;
		}

		static int getInitCount() {
			return initCount;
		}

		static int getDestroyCount() {
			return destroyCount;
		}

		void setDelay(long delay) {
			this.delay = delay;
		}

		@Override
		void afterPropertiesSet() throws Exception {
			logger.debug("Initializing message: " + this.message);
			initCount++;
		}

		@Override
		void destroy() throws Exception {
			logger.debug("Destroying message: " + this.message);
			destroyCount++;
			this.message = null;
		}

		@Override
		String getMessage() {
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

		void setMessage(String message) {
			logger.debug("Setting message: " + message);
			this.message = message;
		}

		@Override
		String throwsException() throws ServiceException {
			throw new ServiceException();
		}

		@Override
		void onApplicationEvent(RefreshScopeRefreshedEvent e) {
			event = e;
		}

	}

	@SuppressWarnings("serial")
	static class ServiceException extends Exception {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(TestProperties.class)
	@EnableAutoConfiguration
	protected static class TestConfiguration {

		@Autowired
		private TestProperties properties;

		@Bean
		@RefreshScope
		ExampleService service() {
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
		String getMessage() {
			return this.message;
		}

		void setMessage(String message) {
			this.message = message;
		}

		@ManagedAttribute
		int getDelay() {
			return this.delay;
		}

		void setDelay(int delay) {
			this.delay = delay;
		}

	}

}
