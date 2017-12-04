package org.springframework.cloud.context.named;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

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
		assertThat("foo was null", foo, is(notNullValue()));

		Bar bar = factory.getInstance("bar", Bar.class);
		assertThat("bar was null", bar, is(notNullValue()));

		assertThat("context names not exposed", factory.getContextNames(), hasItems("foo", "bar"));

		Bar foobar = factory.getInstance("foo", Bar.class);
		assertThat("bar was not null", foobar, is(nullValue()));

		Map<String, Baz> fooBazes = factory.getInstances("foo", Baz.class);
		assertThat("fooBazes was null", fooBazes, is(notNullValue()));
		assertThat("fooBazes size was wrong", fooBazes.size(), is(1));

		Map<String, Baz> barBazes = factory.getInstances("bar", Baz.class);
		assertThat("barBazes was null", barBazes, is(notNullValue()));
		assertThat("barBazes size was wrong", barBazes.size(), is(2));

		// get the contexts before destroy() to verify these are the old ones
		AnnotationConfigApplicationContext fooContext = factory.getContext("foo");
		AnnotationConfigApplicationContext barContext = factory.getContext("bar");

		factory.destroy();

		assertThat("foo context wasn't closed", fooContext.isActive(), is(false));

		assertThat("bar context wasn't closed", barContext.isActive(), is(false));
	}

	private TestSpec getSpec(String name, Class<?> configClass) {
		return new TestSpec(name, new Class[]{configClass});
	}

	static class TestClientFactory extends NamedContextFactory<TestSpec> {

		public TestClientFactory() {
			super(TestSpec.class, "testfactory", "test.client.name");
		}
	}

	static class TestSpec implements NamedContextFactory.Specification {
		private String name;

		private Class<?>[] configuration;

		public TestSpec() {
		}

		public TestSpec(String name, Class<?>[] configuration) {
			this.name = name;
			this.configuration = configuration;
		}

		@Override
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public Class<?>[] getConfiguration() {
			return configuration;
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
	static class Baz{}

	static class FooConfig {
		@Bean
		Foo foo() {
			return new Foo();
		}
	}
	static class Foo{}

	static class BarConfig {
		@Bean
		Bar bar() {
			return new Bar();
		}

		@Bean
		Baz baz2() {
			return new Baz();
		}
	}
	static class Bar{}

}
