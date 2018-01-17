/*
 * Copyright 2013-2015 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Dave Syer
 */
public class RefreshScopeSerializationTests {

	@Test
	public void defaultApplicationContextId() throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).run();
		assertThat(context.getId(), is(equalTo("application-1")));
	}

	@Test
	public void serializationIdReproducible() throws Exception {
		String first = getBeanFactory().getSerializationId();
		String second = getBeanFactory().getSerializationId();
		assertThat(first, is(notNullValue()));
		assertThat(first, is(not(equalTo("application"))));
		assertThat(first, is(equalTo(second)));
	}

	private DefaultListableBeanFactory getBeanFactory() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfiguration.class).web(WebApplicationType.NONE).run();
		DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context
				.getAutowireCapableBeanFactory();
		return factory;
	}

	@Configuration
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
