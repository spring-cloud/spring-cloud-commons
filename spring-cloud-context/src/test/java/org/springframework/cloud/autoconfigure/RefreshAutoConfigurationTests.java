/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.cloud.autoconfigure;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 * @author Yanming Zhou
 */
@ExtendWith(OutputCaptureExtension.class)
class RefreshAutoConfigurationTests {

	private static ConfigurableApplicationContext getApplicationContext(WebApplicationType type, Class<?> configuration,
			String... properties) {
		return new SpringApplicationBuilder(configuration).web(type)
			.properties(properties)
			.properties("server.port=0")
			.run();
	}

	@Test
	void noWarnings(CapturedOutput output) {
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.NONE, Config.class)) {
			then(context.containsBean("refreshScope")).isTrue();
			then(output.toString()).doesNotContain("WARN");
		}
	}

	@Test
	void disabled() {
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.SERVLET, Config.class,
				"spring.cloud.refresh.enabled:false")) {
			then(context.containsBean("refreshScope")).isFalse();
		}
	}

	@Test
	void refreshables() {
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.NONE, Config.class,
				"config.foo=bar", "spring.cloud.refresh.refreshable:" + SealedConfigProps.class.getName())) {
			context.getBean(SealedConfigProps.class);
			context.getBean(ContextRefresher.class).refresh();
		}
	}

	@Test
	public void extraRefreshableWithClassName() {
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.NONE, Config.class,
				"sealedconfig.foo=bar",
				"spring.cloud.refresh.extra-refreshable:" + SealedConfigProps.class.getName())) {
			context.getBean(SealedConfigProps.class);
			context.getBean(ContextRefresher.class).refresh();
		}
	}

	@Test
	public void extraRefreshableWithBeanName() {
		String beanName = "sealedconfig-" + SealedConfigProps.class.getName();
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.NONE, Config.class,
				"sealedconfig.foo=bar", "spring.cloud.refresh.extra-refreshable:" + beanName)) {
			context.getBean(SealedConfigProps.class);
			context.getBean(ContextRefresher.class).refresh();
		}
	}

	@Test
	void neverRefreshableWithClassName() {
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.NONE, Config.class,
				"countingconfig.foo=bar",
				"spring.cloud.refresh.never-refreshable:" + CountingConfigProps.class.getName())) {
			CountingConfigProps configProps = context.getBean(CountingConfigProps.class);
			context.getBean(ContextRefresher.class).refresh();
			assertThat(configProps.count).as("config props was rebound when it should not have been").hasValue(1);
		}
	}

	@Test
	void neverRefreshableWithBeanName() {
		String beanName = "countingconfig-" + CountingConfigProps.class.getName();
		try (ConfigurableApplicationContext context = getApplicationContext(WebApplicationType.NONE, Config.class,
				"countingconfig.foo=bar", "spring.cloud.refresh.never-refreshable:" + beanName)) {
			CountingConfigProps configProps = context.getBean(CountingConfigProps.class);
			context.getBean(ContextRefresher.class).refresh();
			assertThat(configProps.count).as("config props was rebound when it should not have been").hasValue(1);
		}
	}

	@Test
	void refreshScopeLifecylePresentByDefault() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
			.run(context -> assertThat(context).hasBean("refreshScopeLifecycle"));
	}

	@Test
	void refreshScopeLifecyleDisabledWithProp() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
			.withPropertyValues("spring.cloud.refresh.on-restart.enabled=false")
			.run(context -> assertThat(context).doesNotHaveBean("refreshScopeLifecycle"));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
	@EnableConfigurationProperties({ SealedConfigProps.class, CountingConfigProps.class })
	static class Config {

	}

	@ConfigurationProperties("sealedconfig")
	static class SealedConfigProps {

		private String foo;

		private boolean sealed;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			if (this.sealed) {
				throw new IllegalStateException("Cannot set sealed property");
			}
			this.foo = foo;
			this.sealed = true;
		}

	}

	@ConfigurationProperties("countingconfig")
	static class CountingConfigProps {

		private final AtomicInteger count = new AtomicInteger();

		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			count.incrementAndGet();
			this.foo = foo;
		}

	}

}
