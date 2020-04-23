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

package org.springframework.cloud.client.actuator;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 */
public class FeaturesEndpointTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JacksonAutoConfiguration.class, FeaturesConfig.class,
				Config.class);
		this.context.refresh();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void invokeWorks() {
		FeaturesEndpoint.Features features = this.context.getBean(FeaturesEndpoint.class)
				.features();
		then(features).isNotNull();
		then(features.getEnabled()).hasSize(2).contains(newFeature("foo", Foo.class),
				newFeature("Baz Feature", Baz.class));
		then(features.getDisabled()).hasSize(1).contains("Bar");
	}

	private FeaturesEndpoint.Feature newFeature(String name, Class<?> type) {
		return new FeaturesEndpoint.Feature(name, type.getCanonicalName(), null, null);
	}

	@Configuration(proxyBeanMethods = false)
	public static class FeaturesConfig {

		@Bean
		Foo foo() {
			return new Foo();
		}

		@Bean
		HasFeatures localFeatures() {
			HasFeatures features = HasFeatures.namedFeatures(
					new NamedFeature("foo", Foo.class),
					new NamedFeature("Baz Feature", Baz.class));
			features.getAbstractFeatures().add(Bar.class);
			return features;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	public static class Config {

		@Autowired(required = false)
		private List<HasFeatures> hasFeatures = new ArrayList<>();

		@Bean
		public FeaturesEndpoint cloudEndpoint() {
			return new FeaturesEndpoint(this.hasFeatures);
		}

	}

	public static class Foo {

	}

	public static class Bar {

	}

	public static class Baz {

	}

}
