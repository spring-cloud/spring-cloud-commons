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

import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentManager;
import org.springframework.cloud.context.scope.refresh.RefreshScopeWebIntegrationTests.Application;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 *
 */
@SpringBootTest(classes = Application.class)
public class RefreshScopeWebIntegrationTests {

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Autowired
	private EnvironmentManager environmentManager;

	@Autowired
	private Client application;

	@Autowired
	private ConfigurableListableBeanFactory beanFactory;

	@Test
	public void scopeOnBeanDefinition() throws Exception {
		then(this.beanFactory.getBeanDefinition(ScopedProxyUtils.getTargetBeanName("application")).getScope())
				.isEqualTo("refresh");
	}

	@Test
	public void beanAccess() throws Exception {
		this.application.hello();
		this.environmentManager.setProperty("message", "Hello Dave!");
		this.scope.refreshAll();
		String message = this.application.hello();
		then(message).isEqualTo("Hello Dave!");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class Application {

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

		@Bean
		@RefreshScope
		public Client application() {
			return new Client();
		}

	}

	@RestController
	protected static class Client {

		@Value("${message:Hello World!}")
		String message;

		@GetMapping("/")
		public String hello() {
			return this.message;
		}

	}

}
