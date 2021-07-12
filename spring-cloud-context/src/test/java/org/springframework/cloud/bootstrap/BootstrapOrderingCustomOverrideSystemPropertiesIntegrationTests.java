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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.BootstrapOrderingCustomPropertySourceIntegrationTests.Application;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class,
		properties = { "spring.config.use-legacy-processing=true", "spring.cloud.bootstrap.name:ordering" })
public class BootstrapOrderingCustomOverrideSystemPropertiesIntegrationTests {

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	public void orderingIsCorrect() {
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> test1 = propertySources.get("bootstrapProperties-testBootstrap1");
		PropertySource<?> test2 = propertySources.get("bootstrapProperties-testBootstrap2");
		PropertySource<?> test3 = propertySources.get("bootstrapProperties-testBootstrap3");
		int index1 = propertySources.precedenceOf(test1);
		int index2 = propertySources.precedenceOf(test2);
		int index3 = propertySources.precedenceOf(test3);
		assertThat(index1).as("source1 index not less then source2").isLessThan(index2);
		assertThat(index2).as("source2 index not less then source3").isLessThan(index3);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	protected static class Application {

	}

	@Configuration(proxyBeanMethods = false)
	// This is added to bootstrap context as a source in ordering.properties
	protected static class PropertySourceConfiguration implements PropertySourceLocator {

		@Override
		public PropertySource<?> locate(Environment environment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<PropertySource<?>> locateCollection(Environment environment) {
			ArrayList<PropertySource<?>> sources = new ArrayList<>();
			sources.add(new MapPropertySource("testBootstrap1", singletonMap("key1", "value1")));
			sources.add(new MapPropertySource("testBootstrap2", singletonMap("key2", "value2")));
			Map<String, Object> map = new HashMap<>();
			map.put("key3", "value3");
			map.put("spring.cloud.config.override-system-properties", "false");
			sources.add(new MapPropertySource("testBootstrap3", map));
			return sources;
		}

	}

}
