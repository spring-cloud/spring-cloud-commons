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

package org.springframework.cloud.context.named;

import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 */
public class NamedContextFactoryTests {

	@Test
	public void testChildContexts() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(BaseConfig.class);
		parent.refresh();
		TestClientFactory factory = new TestClientFactory();
		factory.setApplicationContext(parent);
		factory.setConfigurations(Arrays.asList(getSpec("foo", FooConfig.class),
				getSpec("bar", BarConfig.class)));

		Foo foo = factory.getInstance("foo", Foo.class);
		then(foo).as("foo was null").isNotNull();

		Bar bar = factory.getInstance("bar", Bar.class);
		then(bar).as("bar was null").isNotNull();

		then(factory.getContextNames()).as("context names not exposed").contains("foo",
				"bar");

		Bar foobar = factory.getInstance("foo", Bar.class);
		then(foobar).as("bar was not null").isNull();

		Baz fooBaz = factory.getInstance("foo", Baz.class);
		then(fooBaz).as("fooBaz was null").isNotNull();

		Object fooContainerFoo = factory.getInstance("foo", Container.class, Foo.class);
		then(fooContainerFoo).as("fooContainerFoo was null").isNotNull();

		Object fooContainerBar = factory.getInstance("foo", Container.class, Bar.class);
		then(fooContainerBar).as("fooContainerBar was not null").isNull();

		Object barContainerBar = factory.getInstance("bar", Container.class, Bar.class);
		then(barContainerBar).as("barContainerBar was null").isNotNull();

		Map<String, Baz> fooBazes = factory.getInstances("foo", Baz.class);
		then(fooBazes).as("fooBazes was null").isNotNull();
		then(fooBazes.size()).as("fooBazes size was wrong").isEqualTo(1);

		Map<String, Baz> barBazes = factory.getInstances("bar", Baz.class);
		then(barBazes).as("barBazes was null").isNotNull();
		then(barBazes.size()).as("barBazes size was wrong").isEqualTo(2);

		// get the contexts before destroy() to verify these are the old ones
		AnnotationConfigApplicationContext fooContext = factory.getContext("foo");
		AnnotationConfigApplicationContext barContext = factory.getContext("bar");

		then(fooContext.getClassLoader())
				.as("foo context classloader does not match parent")
				.isSameAs(parent.getClassLoader());

		Assertions.assertThat(fooContext).hasFieldOrPropertyWithValue("customClassLoader",
				true);

		factory.destroy();

		then(fooContext.isActive()).as("foo context wasn't closed").isFalse();

		then(barContext.isActive()).as("bar context wasn't closed").isFalse();
	}

	private TestSpec getSpec(String name, Class<?> configClass) {
		return new TestSpec(name, new Class[] { configClass });
	}

	static class TestClientFactory extends NamedContextFactory<TestSpec> {

		TestClientFactory() {
			super(TestSpec.class, "testfactory", "test.client.name");
		}

	}

	static class TestSpec implements NamedContextFactory.Specification {

		private String name;

		private Class<?>[] configuration;

		TestSpec() {
		}

		TestSpec(String name, Class<?>[] configuration) {
			this.name = name;
			this.configuration = configuration;
		}

		@Override
		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public Class<?>[] getConfiguration() {
			return this.configuration;
		}

		public void setConfiguration(Class<?>[] configuration) {
			this.configuration = configuration;
		}

	}

	static class BaseConfig {

		@Bean
		Baz baz1() {
			return new Baz();
		}

	}

	static class Baz {

	}

	static class FooConfig {

		@Bean
		Foo foo() {
			return new Foo();
		}

		@Bean
		Container<Foo> fooContainer() {
			return new Container<>(new Foo());
		}

	}

	static class Foo {

	}

	static class BarConfig {

		@Bean
		Bar bar() {
			return new Bar();
		}

		@Bean
		Baz baz2() {
			return new Baz();
		}

		@Bean
		Container<Bar> barContainer() {
			return new Container<>(new Bar());
		}

	}

	static class Bar {

	}

	static class Container<T> {

		private final T item;

		Container(T item) {
			this.item = item;
		}

		public T getItem() {
			return this.item;
		}

	}

}
