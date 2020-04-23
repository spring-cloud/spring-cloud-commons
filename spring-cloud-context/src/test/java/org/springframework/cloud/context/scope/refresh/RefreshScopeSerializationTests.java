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

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 */
public class RefreshScopeSerializationTests {

	@Test
	public void defaultApplicationContextId() throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).run();
		then(context.getId()).isEqualTo("application-1");
	}

	@Test
	public void serializationIdReproducible() throws Exception {
		String first = getBeanFactory().getSerializationId();
		String second = getBeanFactory().getSerializationId();
		then(first).isNotNull();
		then(first).isNotEqualTo("application");
		then(first).isEqualTo(second);
	}

	private DefaultListableBeanFactory getBeanFactory() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).run();
		DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context
				.getAutowireCapableBeanFactory();
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfiguration {

		@Bean
		public RefreshScope refreshScope() {
			return new RefreshScope();
		}

		@Bean
		@org.springframework.cloud.context.config.annotation.RefreshScope
		public TestBean testBean() {
			return new TestBean();
		}

	}

	protected static class TestBean {

	}

}
