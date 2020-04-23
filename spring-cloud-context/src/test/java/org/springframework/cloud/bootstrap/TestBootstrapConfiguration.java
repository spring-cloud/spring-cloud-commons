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

package org.springframework.cloud.bootstrap;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import static org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration.firstToBeCreated;

/**
 * @author Spencer Gibb
 */
@Order(0)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
public class TestBootstrapConfiguration {

	public static List<String> fooSightings = null;

	public TestBootstrapConfiguration() {
		firstToBeCreated.compareAndSet(null, TestBootstrapConfiguration.class);
	}

	@Bean
	@Qualifier("foo-during-bootstrap")
	public String fooDuringBootstrap(ConfigurableEnvironment environment,
			ApplicationEventPublisher publisher) {
		String property = environment.getProperty("test.bootstrap.foo", "undefined");

		if (fooSightings != null) {
			fooSightings.add(property);
		}

		return property;
	}

	@Bean
	public ApplicationContextInitializer<ConfigurableApplicationContext> customInitializer() {
		return new ApplicationContextInitializer<ConfigurableApplicationContext>() {

			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {
				ConfigurableEnvironment environment = applicationContext.getEnvironment();
				environment.getPropertySources()
						.addLast(new MapPropertySource("customProperties",
								Collections.<String, Object>singletonMap("custom.foo",
										environment.resolvePlaceholders(
												"${spring.application.name:bar}"))));
			}

		};
	}

}
