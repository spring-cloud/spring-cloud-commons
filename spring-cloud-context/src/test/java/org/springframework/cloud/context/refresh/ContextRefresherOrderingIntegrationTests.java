/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.context.refresh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class ContextRefresherOrderingIntegrationTests {

	@Autowired
	private ConfigurableEnvironment environment;

	@Autowired
	private ContextRefresher refresher;

	private static String original;

	@BeforeClass
	public static void beforeClass() {
		original = System.getProperty("spring.cloud.bootstrap.sources");
		System.setProperty("spring.cloud.bootstrap.sources",
				"org.springframework.cloud.context.refresh.ContextRefresherOrderingIntegrationTests.PropertySourceConfiguration");
	}

	@AfterClass
	public static void afterClass() {
		if (original != null) {
			System.setProperty("spring.cloud.bootstrap.sources", original);
		}
		else {
			System.clearProperty("spring.cloud.bootstrap.sources");
		}
	}

	@Test
	public void orderingIsCorrect() {
		refresher.refresh();
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> test1 = propertySources
				.get("bootstrapProperties-testContextRefresherOrdering1");
		PropertySource<?> test2 = propertySources
				.get("bootstrapProperties-testContextRefresherOrdering2");
		PropertySource<?> test3 = propertySources
				.get("bootstrapProperties-testContextRefresherOrdering3");
		int index1 = propertySources.precedenceOf(test1);
		int index2 = propertySources.precedenceOf(test2);
		int index3 = propertySources.precedenceOf(test3);
		assertThat(index1).as("source1 index not less then source2").isLessThan(index2);
		assertThat(index2).as("source2 index not less then source3").isLessThan(index3);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class Application {

	}

	@Configuration(proxyBeanMethods = false)
	// This is added to bootstrap context as a source in
	// contextrefresherordering.properties
	protected static class PropertySourceConfiguration implements PropertySourceLocator {

		private static AtomicBoolean first = new AtomicBoolean(true);

		@Override
		public PropertySource<?> locate(Environment environment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<PropertySource<?>> locateCollection(Environment environment) {
			if (first.compareAndSet(true, false)) {
				return Collections.emptyList();
			}
			ArrayList<PropertySource<?>> sources = new ArrayList<>();
			sources.add(new MapPropertySource("testContextRefresherOrdering1",
					singletonMap("key1", "value1")));
			sources.add(new MapPropertySource("testContextRefresherOrdering2",
					singletonMap("key2", "value2")));
			sources.add(new MapPropertySource("testContextRefresherOrdering3",
					singletonMap("key3", "value3")));
			return sources;
		}

	}

}
