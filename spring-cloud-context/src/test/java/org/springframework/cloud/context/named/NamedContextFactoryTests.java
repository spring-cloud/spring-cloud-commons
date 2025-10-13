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

package org.springframework.cloud.context.named;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 * @author Tommy Karlsson
 * @author Olga Maciaszek-Sharma
 */
public class NamedContextFactoryTests {

	@Test
	public void testChildContexts() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(BaseConfig.class);
		parent.refresh();
		testChildContexts(parent);
	}

	@Test
	void testBadThreadContextClassLoader() throws InterruptedException, ExecutionException, TimeoutException {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.setClassLoader(ClassUtils.getDefaultClassLoader());
		parent.register(BaseConfig.class);
		parent.refresh();

		ExecutorService es = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r);
			t.setContextClassLoader(new ThrowingClassLoader());
			return t;
		});
		es.submit(() -> this.testChildContexts(parent)).get(5, TimeUnit.SECONDS);

	}

	@Test
	void testGetAnnotatedBeanInstance() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(BaseConfig.class);
		parent.refresh();
		TestClientFactory factory = new TestClientFactory();
		factory.setApplicationContext(parent);
		factory.setConfigurations(List.of(getSpec("annotated", AnnotatedConfig.class)));

		TestType annotatedBean = factory.getAnnotatedInstance("annotated", ResolvableType.forType(TestType.class),
				TestBean.class);

		assertThat(annotatedBean.value()).isEqualTo(2);
	}

	@Test
	void testNoAnnotatedBeanInstance() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(BaseConfig.class);
		parent.refresh();
		TestClientFactory factory = new TestClientFactory();
		factory.setApplicationContext(parent);
		factory.setConfigurations(List.of(getSpec("not-annotated", NotAnnotatedConfig.class)));

		TestType annotatedBean = factory.getAnnotatedInstance("not-annotated", ResolvableType.forType(TestType.class),
				TestBean.class);
		assertThat(annotatedBean).isNull();
	}

	@Test
	void testMoreThanOneAnnotatedBeanInstance() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(BaseConfig.class);
		parent.refresh();
		TestClientFactory factory = new TestClientFactory();
		factory.setApplicationContext(parent);
		factory.setConfigurations(List.of(getSpec("many-annotated", ManyAnnotatedConfig.class)));

		assertThatIllegalStateException().isThrownBy(() -> factory.getAnnotatedInstance("many-annotated",
				ResolvableType.forType(TestType.class), TestBean.class));
	}

	private void testChildContexts(GenericApplicationContext parent) {
		TestClientFactory factory = new TestClientFactory();
		factory.setApplicationContext(parent);
		factory.setConfigurations(Arrays.asList(getSpec("foo", FooConfig.class), getSpec("bar", BarConfig.class)));

		Foo foo = factory.getInstance("foo", Foo.class);
		then(foo).as("foo was null").isNotNull();

		Bar bar = factory.getInstance("bar", Bar.class);
		then(bar).as("bar was null").isNotNull();

		then(factory.getContextNames()).as("context names not exposed").contains("foo", "bar");

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
		GenericApplicationContext fooContext = factory.getContext("foo");
		GenericApplicationContext barContext = factory.getContext("bar");

		then(fooContext.getClassLoader()).as("foo context classloader does not match parent")
			.isSameAs(parent.getClassLoader());

		then(fooContext.getBeanFactory().getBeanClassLoader())
			.as("foo context bean factory classloader does not match parent")
			.isSameAs(parent.getBeanFactory().getBeanClassLoader());

		assertThat(fooContext).hasFieldOrPropertyWithValue("customClassLoader", true);

		factory.destroy();

		then(fooContext.isActive()).as("foo context wasn't closed").isFalse();

		then(barContext.isActive()).as("bar context wasn't closed").isFalse();
	}

	private TestSpec getSpec(String name, Class<?> configClass) {
		return new TestSpec(name, new Class[] { configClass });
	}

	static class ThrowingClassLoader extends ClassLoader {

		ThrowingClassLoader() {
			super(null);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}

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

	@ConditionalOnClass(Object.class)
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

	record Container<T>(T item) {

	}

	static class AnnotatedConfig {

		@Bean
		TestType test1() {
			return new TestType(1);
		}

		@TestBean
		@Bean
		TestType test2() {
			return new TestType(2);
		}

		@TestBean
		@Bean
		Bar bar() {
			return new Bar();
		}

	}

	static class NotAnnotatedConfig {

		@Bean
		TestType test1() {
			return new TestType(1);
		}

		@Bean
		TestType test2() {
			return new TestType(2);
		}

		@TestBean
		@Bean
		Bar bar() {
			return new Bar();
		}

	}

	static class ManyAnnotatedConfig {

		@TestBean
		@Bean
		TestType test1() {
			return new TestType(1);
		}

		@TestBean
		@Bean
		TestType test2() {
			return new TestType(2);
		}

		@Bean
		Bar bar() {
			return new Bar();
		}

	}

	record TestType(int value) {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	@interface TestBean {

	}

}
