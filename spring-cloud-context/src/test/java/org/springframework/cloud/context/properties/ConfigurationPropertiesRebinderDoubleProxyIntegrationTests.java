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

package org.springframework.cloud.context.properties;

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderDoubleProxyIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that a {@code @ConfigurationProperties} bean that is an AOP proxy around
 * another AOP proxy (for example a {@code @Validated} bean, proxied for method
 * validation, that an application aspect then proxies again) can be rebound, and that the
 * advice of both proxies is kept. Reproduces
 * <a href="https://github.com/spring-cloud/spring-cloud-commons/issues/1780">gh-1780</a>.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class, properties = "test.message=Hello")
public class ConfigurationPropertiesRebinderDoubleProxyIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void rebindKeepsBothProxies() {
		TestProperties properties = this.context.getBean("testProperties", TestProperties.class);
		then(properties.getMessage()).isEqualTo("Hello");
		TestPropertyValues.of("test.message=World").applyTo(this.environment);
		int innerBefore = TestConfiguration.INNER_CALLS.get();
		int outerBefore = TestConfiguration.OUTER_CALLS.get();

		this.rebinder.rebind();

		then(this.rebinder.getErrors()).isEmpty();
		then(this.context.getBean("testProperties")).isSameAs(properties);
		then(properties.getMessage()).isEqualTo("World");
		// Rebinding must not have bypassed the advice of either proxy.
		then(TestConfiguration.INNER_CALLS.get()).isGreaterThan(innerBefore);
		then(TestConfiguration.OUTER_CALLS.get()).isGreaterThan(outerBefore);
	}

	@Test
	@DirtiesContext
	public void neverRefreshableMatchesTheClassBehindBothProxies() {
		TestProperties properties = this.context.getBean("testProperties", TestProperties.class);
		TestPropertyValues
			.of("test.message=World", "spring.cloud.refresh.never-refreshable=" + TestProperties.class.getName())
			.applyTo(this.environment);

		then(this.rebinder.rebind("testProperties")).isFalse();

		then(properties.getMessage()).isEqualTo("Hello");
	}

	static Object proxy(Object target, AtomicInteger calls) {
		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(true);
		factory.addAdvice((MethodInterceptor) invocation -> {
			calls.incrementAndGet();
			return invocation.proceed();
		});
		return factory.getProxy();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		static final AtomicInteger INNER_CALLS = new AtomicInteger();

		static final AtomicInteger OUTER_CALLS = new AtomicInteger();

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties();
		}

		@Bean
		protected static InnerProxyingBeanPostProcessor innerProxyingBeanPostProcessor() {
			return new InnerProxyingBeanPostProcessor();
		}

		@Bean
		protected static OuterProxyingBeanPostProcessor outerProxyingBeanPostProcessor() {
			return new OuterProxyingBeanPostProcessor();
		}

	}

	// Hack out a protected inner class for testing
	protected static class RefreshConfiguration extends RefreshAutoConfiguration {

		@Configuration(proxyBeanMethods = false)
		protected static class RebinderConfiguration extends ConfigurationPropertiesRebinderAutoConfiguration {

			public RebinderConfiguration(ApplicationContext context) {
				super(context);
			}

		}

	}

	/**
	 * Stands in for the method validation post-processor.
	 */
	protected static class InnerProxyingBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return (bean instanceof TestProperties) ? proxy(bean, TestConfiguration.INNER_CALLS) : bean;
		}

	}

	/**
	 * Stands in for an auto-proxy creator, which wraps the already proxied bean.
	 */
	protected static class OuterProxyingBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return (bean instanceof TestProperties) ? proxy(bean, TestConfiguration.OUTER_CALLS) : bean;
		}

	}

	@ConfigurationProperties("test")
	public static class TestProperties {

		private String message;

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

}
