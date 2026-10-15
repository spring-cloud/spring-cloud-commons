/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.support;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link LoadBalancerClientFactory}.
 */
class LoadBalancerClientFactoryTests {

	private final LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory(
			new LoadBalancerClientsProperties());

	private final AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();

	@BeforeEach
	void setUp() {
		parent.registerBean("parentTestBean", TestBean.class, () -> new TestBean("parent"));
		parent.registerBean(LoadBalancerClientFactory.class, () -> clientFactory);
		parent.refresh();
		clientFactory.setApplicationContext(parent);
	}

	@AfterEach
	void tearDown() {
		clientFactory.destroy();
		parent.close();
	}

	@Test
	void shouldReturnInstanceDeclaredForClientWithoutAncestors() {
		clientFactory.setConfigurations(
				List.of(new LoadBalancerClientSpecification("test", new Class<?>[] { ClientConfiguration.class })));

		TestBean bean = clientFactory.getInstanceWithoutAncestors("test", TestBean.class);

		assertThat(bean).isNotNull();
		assertThat(bean.source()).isEqualTo("client");
	}

	@Test
	void shouldNotReturnInstanceFromParentContextWithoutAncestors() {
		assertThat(clientFactory.getInstance("test", TestBean.class)).isNotNull();
		assertThat(clientFactory.getInstanceWithoutAncestors("test", TestBean.class)).isNull();
	}

	@Test
	void shouldReturnPrimaryInstanceWithoutAncestors() {
		clientFactory.setConfigurations(List
			.of(new LoadBalancerClientSpecification("test", new Class<?>[] { PrimaryClientConfiguration.class })));

		TestBean bean = clientFactory.getInstanceWithoutAncestors("test", TestBean.class);

		assertThat(bean).isNotNull();
		assertThat(bean.source()).isEqualTo("primary");
	}

	@Test
	void shouldPropagateCreationFailureWithoutAncestors() {
		clientFactory.setConfigurations(List
			.of(new LoadBalancerClientSpecification("test", new Class<?>[] { FailingClientConfiguration.class })));

		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(() -> clientFactory.getInstanceWithoutAncestors("test", TestBean.class));
	}

	record TestBean(String source) {
	}

	@Configuration(proxyBeanMethods = false)
	static class ClientConfiguration {

		@Bean
		TestBean clientTestBean() {
			return new TestBean("client");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PrimaryClientConfiguration {

		@Bean
		TestBean clientTestBean() {
			return new TestBean("client");
		}

		@Bean
		@Primary
		TestBean primaryTestBean() {
			return new TestBean("primary");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FailingClientConfiguration {

		@Bean
		@Lazy
		TestBean clientTestBean() {
			throw new IllegalStateException("broken");
		}

	}

}
