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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderTransientStateIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.PriorityOrdered;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Verifies that a reader never observes the unbound, class-default state of a
 * {@code @ConfigurationProperties} bean while it is being rebound (gh-1727). A
 * {@link PriorityOrdered} {@link BeanPostProcessor} that runs ahead of the binding
 * post-processor reads the bean from inside the rebind, which is where a concurrent
 * request thread would land.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = TestConfiguration.class, properties = { "test.timeout.connect=5", "test.retries=4" })
public class ConfigurationPropertiesRebinderTransientStateIntegrationTests {

	@Autowired
	private ConfigurationPropertiesRebinder rebinder;

	@Test
	@DirtiesContext
	public void nestedObjectIsNeverNullDuringRebind() {
		ReadingPostProcessor.reset();
		ReadingPostProcessor.enabled.set(true);
		this.rebinder.rebind("testProperties");
		then(ReadingPostProcessor.reads.get()).isGreaterThan(0);
		then(ReadingPostProcessor.nullTimeouts.get()).as("reads that saw a null nested object").isZero();
	}

	@Test
	@DirtiesContext
	public void boundScalarIsNeverUnboundDuringRebind() {
		ReadingPostProcessor.reset();
		ReadingPostProcessor.enabled.set(true);
		this.rebinder.rebind("testProperties");
		then(ReadingPostProcessor.reads.get()).isGreaterThan(0);
		then(ReadingPostProcessor.zeroRetries.get()).as("reads that saw the unbound default").isZero();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshConfiguration.RebinderConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		protected TestProperties testProperties() {
			return new TestProperties();
		}

		@Bean
		protected static ReadingPostProcessor readingPostProcessor() {
			return new ReadingPostProcessor();
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

	protected static class ReadingPostProcessor implements BeanPostProcessor, PriorityOrdered {

		static final AtomicBoolean enabled = new AtomicBoolean();

		static final AtomicInteger reads = new AtomicInteger();

		static final AtomicInteger nullTimeouts = new AtomicInteger();

		static final AtomicInteger zeroRetries = new AtomicInteger();

		static void reset() {
			enabled.set(false);
			reads.set(0);
			nullTimeouts.set(0);
			zeroRetries.set(0);
		}

		@Override
		public int getOrder() {
			return HIGHEST_PRECEDENCE;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (enabled.get() && bean instanceof TestProperties properties) {
				reads.incrementAndGet();
				if (properties.getTimeout() == null) {
					nullTimeouts.incrementAndGet();
				}
				if (properties.getRetries() == 0) {
					zeroRetries.incrementAndGet();
				}
			}
			return bean;
		}

	}

	@ConfigurationProperties("test")
	protected static class TestProperties {

		// populated only by binding
		private Timeout timeout;

		private int retries;

		public Timeout getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Timeout timeout) {
			this.timeout = timeout;
		}

		public int getRetries() {
			return this.retries;
		}

		public void setRetries(int retries) {
			this.retries = retries;
		}

		protected static class Timeout {

			private int connect;

			public int getConnect() {
				return this.connect;
			}

			public void setConnect(int connect) {
				this.connect = connect;
			}

		}

	}

}
