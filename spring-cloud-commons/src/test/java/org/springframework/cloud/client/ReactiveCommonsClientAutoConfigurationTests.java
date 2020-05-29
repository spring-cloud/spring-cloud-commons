/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.client;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.actuator.FeaturesEndpoint;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryCompositeHealthContributor;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Tim Ysewyn
 */
public class ReactiveCommonsClientAutoConfigurationTests {

	ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CommonsClientAutoConfiguration.class,
					SimpleReactiveDiscoveryClientAutoConfiguration.class,
					UtilAutoConfiguration.class,
					ReactiveCommonsClientAutoConfiguration.class));

	@Test
	public void beansCreatedNormally() {
		applicationContextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=features")
				.run(context -> {
					then(context.getBean(ReactiveDiscoveryClientHealthIndicator.class))
							.isNotNull();
					then(context
							.getBean(ReactiveDiscoveryCompositeHealthContributor.class))
									.isNotNull();
					then(context.getBean(FeaturesEndpoint.class)).isNotNull();
					then(context.getBeansOfType(HasFeatures.class).values()).isNotEmpty();
				});
	}

	@Test
	public void disableAll() {
		applicationContextRunner
				.withPropertyValues("spring.cloud.discovery.enabled=false",
						"management.endpoints.web.exposure.include=features")
				.run(context -> {
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryCompositeHealthContributor.class);
					// features actuator is independent of discovery
					then(context.getBean(FeaturesEndpoint.class)).isNotNull();
					assertThat(context).doesNotHaveBean(HasFeatures.class);
				});
	}

	@Test
	public void disableReactive() {
		applicationContextRunner
				.withPropertyValues("spring.cloud.discovery.reactive.enabled=false",
						"management.endpoints.web.exposure.include=features")
				.run(context -> {
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryCompositeHealthContributor.class);
					// features actuator is independent of discovery
					then(context.getBean(FeaturesEndpoint.class)).isNotNull();
					assertThat(context).doesNotHaveBean(HasFeatures.class);
				});
	}

	@Test
	public void disableAllIndividually() {
		applicationContextRunner.withPropertyValues(
				"spring.cloud.discovery.client.health-indicator.enabled=false",
				"spring.cloud.discovery.client.composite-indicator.enabled=false",
				"spring.cloud.features.enabled=false").run(context -> {
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryCompositeHealthContributor.class);
					assertThat(context).doesNotHaveBean(FeaturesEndpoint.class);
				});
	}

	@Test
	public void disableHealthIndicator() {
		applicationContextRunner
				.withPropertyValues(
						"spring.cloud.discovery.client.health-indicator.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryCompositeHealthContributor.class);
				});
	}

	@Test
	public void worksWithoutActuator() {
		applicationContextRunner
				.withClassLoader(
						new FilteredClassLoader("org.springframework.boot.actuate"))
				.run(context -> {
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryCompositeHealthContributor.class);
					then(context.getBeansOfType(HasFeatures.class).values()).isEmpty();
				});
	}

	@Test
	public void worksWithoutWebflux() {
		applicationContextRunner
				.withClassLoader(
						new FilteredClassLoader("org.springframework.web.reactive"))
				.run(context -> {
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryCompositeHealthContributor.class);
					assertThat(context).doesNotHaveBean(HasFeatures.class);
				});
	}

	@Test
	public void conditionalOnReactiveDiscoveryEnabledWorks() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(ReactiveDiscoveryEnabledConfig.class);
		contextRunner.withPropertyValues("spring.cloud.discovery.reactive.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(TestBean.class));
		contextRunner.withPropertyValues("spring.cloud.discovery.reactive.enabled=true")
				.run(context -> assertThat(context.getBean(TestBean.class)).isNotNull());
	}

	@TestConfiguration
	@ConditionalOnDiscoveryEnabled
	protected static class DiscoveryEnabledConfig {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	@TestConfiguration
	@ConditionalOnReactiveDiscoveryEnabled
	protected static class ReactiveDiscoveryEnabledConfig {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	private static class TestBean {

	}

}
