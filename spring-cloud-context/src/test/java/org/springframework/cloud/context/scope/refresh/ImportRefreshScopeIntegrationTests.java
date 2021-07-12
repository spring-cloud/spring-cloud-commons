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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.ImportRefreshScopeIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class)
public class ImportRefreshScopeIntegrationTests {

	@Autowired
	org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Autowired
	private ConfigurableListableBeanFactory beanFactory;

	@Autowired
	private ExampleService service;

	@Test
	public void testSimpleProperties() throws Exception {
		then(this.service.getMessage()).isEqualTo("Hello scope!");
		then(this.beanFactory.getBeanDefinition(ScopedProxyUtils.getTargetBeanName("service")).getScope())
				.isEqualTo("refresh");
		then(this.service.getMessage()).isEqualTo("Hello scope!");
	}

	@Configuration(value = "service", proxyBeanMethods = false)
	@RefreshScope
	public static class ExampleService {

		public String getMessage() {
			return "Hello scope!";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ RefreshAutoConfiguration.class, ExampleService.class })
	protected static class TestConfiguration {

	}

}
