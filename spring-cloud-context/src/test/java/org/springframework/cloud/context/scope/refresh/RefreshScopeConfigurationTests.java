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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentManager;
import org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationTests.NestedApp.NestedController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 *
 */
public class RefreshScopeConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	public void init() {
		if (this.context != null) {
			this.context.close();
		}
	}

	private void refresh() {
		EnvironmentManager environmentManager = this.context.getBean(EnvironmentManager.class);
		environmentManager.setProperty("message", "Hello Dave!");
		org.springframework.cloud.context.scope.refresh.RefreshScope scope = this.context
				.getBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class);
		scope.refreshAll();
	}

	/**
	 * See gh-43
	 */
	@Test
	public void configurationWithRefreshScope() throws Exception {
		this.context = new AnnotationConfigApplicationContext(Application.class,
				PropertyPlaceholderAutoConfiguration.class, RefreshAutoConfiguration.class,
				LifecycleMvcEndpointAutoConfiguration.class);
		Application application = this.context.getBean(Application.class);
		then(this.context.getBeanDefinition(ScopedProxyUtils.getTargetBeanName("application")).getScope())
				.isEqualTo("refresh");
		application.hello();
		refresh();
		String message = application.hello();
		then(message).isEqualTo("Hello Dave!");
	}

	@Test
	public void refreshScopeOnBean() throws Exception {
		this.context = new AnnotationConfigApplicationContext(ClientApp.class,
				PropertyPlaceholderAutoConfiguration.class, RefreshAutoConfiguration.class,
				LifecycleMvcEndpointAutoConfiguration.class);
		Controller application = this.context.getBean(Controller.class);
		application.hello();
		refresh();
		String message = application.hello();
		then(message).isEqualTo("Hello Dave!");
	}

	@Test
	public void refreshScopeOnNested() throws Exception {
		this.context = new AnnotationConfigApplicationContext(NestedApp.class,
				PropertyPlaceholderAutoConfiguration.class, RefreshAutoConfiguration.class,
				LifecycleMvcEndpointAutoConfiguration.class);
		NestedController application = this.context.getBean(NestedController.class);
		application.hello();
		refresh();
		String message = application.hello();
		then(message).isEqualTo("Hello Dave!");
	}

	// WTF? Maven can't compile without the FQN on this one (not the others).
	@org.springframework.context.annotation.Configuration
	protected static class NestedApp {

		public static void main(String[] args) {
			SpringApplication.run(ClientApp.class, args);
		}

		@RestController
		@RefreshScope
		protected static class NestedController {

			@Value("${message:Hello World!}")
			String message;

			@GetMapping("/")
			public String hello() {
				return this.message;
			}

		}

	}

	@Configuration(value = "application", proxyBeanMethods = false)
	@RefreshScope
	protected static class Application {

		@Value("${message:Hello World!}")
		String message = "Hello World";

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

		@GetMapping("/")
		public String hello() {
			return this.message;
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ClientApp {

		public static void main(String[] args) {
			SpringApplication.run(ClientApp.class, args);
		}

		@Bean
		@RefreshScope
		public Controller controller() {
			return new Controller();
		}

	}

	@RestController
	protected static class Controller {

		@Value("${message:Hello World!}")
		String message;

		@GetMapping("/")
		// Deliberately use package scope
		String hello() {
			return this.message;
		}

	}

}
