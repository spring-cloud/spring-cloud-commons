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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = { RefreshScopeNullBeanIntegrationTests.TestConfiguration.class })
public class RefreshScopeNullBeanIntegrationTests {

	@Autowired
	private MyCustomComponent myCustomComponent;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Test
	@DirtiesContext
	public void testRefreshBean() {
		then(this.myCustomComponent.optionalService).isNotNull();
		then(this.scope).isNotNull();
		// ...and then refresh, so the bean is re-initialized:
		// this.scope.refreshAll();
	}

	protected static class OptionalService {

	}

	public static class MyCustomComponent {

		private final OptionalService optionalService;

		public MyCustomComponent(OptionalService optionalService) {
			this.optionalService = optionalService;
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class OptionalConfiguration {

		@Bean
		@RefreshScope
		public OptionalService service() {
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ RefreshAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class, OptionalConfiguration.class })
	protected static class TestConfiguration {

		@Autowired(required = false)
		private OptionalService optionalService;

		@Bean
		public MyCustomComponent myCustomComponent() {
			return new MyCustomComponent(this.optionalService);
		}

	}

}
