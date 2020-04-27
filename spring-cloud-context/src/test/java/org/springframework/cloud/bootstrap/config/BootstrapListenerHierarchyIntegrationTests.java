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

package org.springframework.cloud.bootstrap.config;

import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.WebApplicationType.NONE;

/**
 * Integration tests for Bootstrap Listener's functionality of adding a bootstrap context
 * as the root Application Context.
 *
 * @author Biju Kunjummen
 */
public class BootstrapListenerHierarchyIntegrationTests {

	@Test
	public void shouldAddInABootstrapContext() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(BasicConfiguration.class).web(NONE).run();

		then(context.getParent()).isNotNull();
	}

	@Test
	public void shouldAddInOneBootstrapForABasicParentChildHierarchy() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(RootConfiguration.class).web(NONE)
				.child(BasicConfiguration.class).web(NONE).run();

		// Should be RootConfiguration based context
		ConfigurableApplicationContext parent = (ConfigurableApplicationContext) context
				.getParent();
		then(parent.getBean("rootBean", String.class)).isEqualTo("rootBean");

		// Parent should have the bootstrap context as parent
		then(parent.getParent()).isNotNull();

		ConfigurableApplicationContext bootstrapContext = (ConfigurableApplicationContext) parent
				.getParent();

		// Bootstrap should be the root, there should be no other parent
		then(bootstrapContext.getParent()).isNull();
	}

	@Test
	public void shouldAddInOneBootstrapForSiblingsBasedHierarchy() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(RootConfiguration.class).web(NONE)
				.child(BasicConfiguration.class).web(NONE)
				.sibling(BasicConfiguration.class).web(NONE).run();

		// Should be RootConfiguration based context
		ConfigurableApplicationContext parent = (ConfigurableApplicationContext) context
				.getParent();
		then(parent.getBean("rootBean", String.class)).isEqualTo("rootBean");

		// Parent should have the bootstrap context as parent
		then(parent.getParent()).isNotNull();

		ConfigurableApplicationContext bootstrapContext = (ConfigurableApplicationContext) parent
				.getParent();

		// Bootstrap should be the root, there should be no other parent
		then(bootstrapContext.getParent()).isNull();
	}

	@Configuration(proxyBeanMethods = false)
	static class BasicConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class RootConfiguration {

		@Bean
		public String rootBean() {
			return "rootBean";
		}

	}

}
