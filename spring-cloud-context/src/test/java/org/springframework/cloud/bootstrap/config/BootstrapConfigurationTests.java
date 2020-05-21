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

package org.springframework.cloud.bootstrap.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 *
 */
public class BootstrapConfigurationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private ConfigurableApplicationContext context;

	private ConfigurableApplicationContext sibling;

	@After
	public void close() {
		// Expected.* is bound to the PropertySourceConfiguration below
		System.clearProperty("expected.name");
		System.clearProperty("expected.fail");
		// Used to test system properties override
		System.clearProperty("bootstrap.foo");
		PropertySourceConfiguration.MAP.clear();
		CompositePropertySourceConfiguration.MAP1.clear();
		CompositePropertySourceConfiguration.MAP2.clear();
		if (this.context != null) {
			this.context.close();
		}
		if (this.sibling != null) {
			this.sibling.close();
		}
	}

	@Test
	public void pickupOnlyExternalBootstrapProperties() {
		String externalPropertiesPath = getExternalProperties();

		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class)
				.properties("spring.cloud.bootstrap.location=" + externalPropertiesPath)
				.run();
		then(this.context.getEnvironment().getProperty("info.name"))
				.isEqualTo("externalPropertiesInfoName");
		then(this.context.getEnvironment().getProperty("info.desc")).isNull();
		then(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
						+ "-testBootstrap")).isTrue();
	}

	@Test
	public void pickupAdditionalExternalBootstrapProperties() {
		String externalPropertiesPath = getExternalProperties();

		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class)
				.properties("spring.cloud.bootstrap.additional-location="
						+ externalPropertiesPath)
				.run();
		then(this.context.getEnvironment().getProperty("info.name"))
				.isEqualTo("externalPropertiesInfoName");
		then(this.context.getEnvironment().getProperty("info.desc"))
				.isEqualTo("defaultPropertiesInfoDesc");
		then(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
						+ "-testBootstrap")).isTrue();
	}

	@Test
	public void bootstrapPropertiesAvailableInInitializer() {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).initializers(
						new ApplicationContextInitializer<ConfigurableApplicationContext>() {
							@Override
							public void initialize(
									ConfigurableApplicationContext applicationContext) {
								// This property is defined in bootstrap.properties
								then(applicationContext.getEnvironment()
										.getProperty("info.name")).isEqualTo("child");
							}
						})
				.run();
		then(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
						+ "-testBootstrap")).isTrue();
	}

	/**
	 * Running the test from maven will start from a different directory then starting it
	 * from intellij
	 * @return
	 */
	private String getExternalProperties() {
		String externalPropertiesPath = "classpath:external-properties/bootstrap.properties";
		return externalPropertiesPath;
	}

	@Test
	public void picksUpAdditionalPropertySource() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		then(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
						+ "-testBootstrap")).isTrue();
	}

	@Test
	public void failsOnPropertySource() {
		System.setProperty("expected.fail", "true");
		this.expected.expectMessage("Planned");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
	}

	@Test
	public void overrideSystemPropertySourceByDefault() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
	}

	@Test
	public void systemPropertyOverrideFalse() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP
				.put("spring.cloud.config.overrideSystemProperties", "false");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo"))
				.isEqualTo("system");
	}

	@Test
	public void systemPropertyOverrideWhenOverrideDisallowed() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP
				.put("spring.cloud.config.overrideSystemProperties", "false");
		// If spring.cloud.config.allowOverride=false is in the remote property sources
		// with sufficiently high priority it always wins. Admins can enforce it by adding
		// their own remote property source.
		PropertySourceConfiguration.MAP.put("spring.cloud.config.allowOverride", "false");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
	}

	@Test
	public void systemPropertyOverrideFalseWhenOverrideAllowed() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP
				.put("spring.cloud.config.overrideSystemProperties", "false");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.allowOverride", "true");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo"))
				.isEqualTo("system");
	}

	@Test
	public void overrideAllWhenOverrideAllowed() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.overrideNone", "true");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.allowOverride", "true");
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(new MapPropertySource("last",
				Collections.<String, Object>singletonMap("bootstrap.foo", "splat")));
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.environment(environment).sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo"))
				.isEqualTo("splat");
	}

	@Test
	public void applicationNameInBootstrapAndMain() {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.cloud.bootstrap.name:other",
						"spring.config.name:plain")
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("spring.application.name"))
				.isEqualTo("app");
		// The parent is called "main" because spring.application.name is specified in
		// other.properties (the bootstrap properties)
		then(this.context.getParent().getEnvironment()
				.getProperty("spring.application.name")).isEqualTo("main");
		// The bootstrap context has the same "bootstrap" property source
		then(((ConfigurableEnvironment) this.context.getParent().getEnvironment())
				.getPropertySources().get("bootstrap"))
						.isEqualTo(this.context.getEnvironment().getPropertySources()
								.get("bootstrap"));
		then(this.context.getId()).isEqualTo("main-1");
	}

	@Test
	public void applicationNameNotInBootstrap() {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.cloud.bootstrap.name:application",
						"spring.config.name:other")
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("spring.application.name"))
				.isEqualTo("main");
		// The parent has no name because spring.application.name is not
		// defined in the bootstrap properties
		then(this.context.getParent().getEnvironment()
				.getProperty("spring.application.name")).isEqualTo(null);
	}

	@Test
	public void applicationNameOnlyInBootstrap() {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.cloud.bootstrap.name:other")
				.sources(BareConfiguration.class).run();
		// The main context is called "main" because spring.application.name is specified
		// in other.properties (and not in the main config file)
		then(this.context.getEnvironment().getProperty("spring.application.name"))
				.isEqualTo("main");
		// The parent is called "main" because spring.application.name is specified in
		// other.properties (the bootstrap properties this time)
		then(this.context.getParent().getEnvironment()
				.getProperty("spring.application.name")).isEqualTo("main");
		then(this.context.getId()).isEqualTo("main-1");
	}

	@Test
	public void environmentEnrichedOnceWhenSharedWithChildContext() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.environment(new StandardEnvironment()).child(BareConfiguration.class)
				.web(WebApplicationType.NONE).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		then(this.context.getParent().getEnvironment())
				.isEqualTo(this.context.getEnvironment());
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		PropertySource<?> bootstrap = sources
				.get(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
						+ "-testBootstrap");
		then(bootstrap).isNotNull();
		then(sources.precedenceOf(bootstrap)).isEqualTo(0);
	}

	@Test
	public void onlyOneBootstrapContext() {
		TestHigherPriorityBootstrapConfiguration.count.set(0);
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		then(TestHigherPriorityBootstrapConfiguration.count.get()).isEqualTo(1);
		then(this.context.getParent()).isNotNull();
		then(this.context.getParent().getParent().getId()).isEqualTo("bootstrap");
		then(this.context.getParent().getParent().getParent()).isNull();
		then(this.context.getEnvironment().getProperty("custom.foo")).isEqualTo("bar");
	}

	@Test
	public void listOverride() {
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		ListProperties listProperties = new ListProperties();
		Binder.get(this.context.getEnvironment()).bind("list",
				Bindable.ofInstance(listProperties));
		then(listProperties.getFoo().size()).isEqualTo(1);
		then(listProperties.getFoo().get(0)).isEqualTo("hello world");
	}

	@Test
	public void bootstrapContextSharedBySiblings() {
		TestHigherPriorityBootstrapConfiguration.count.set(0);
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		SpringApplicationBuilder builder = new SpringApplicationBuilder()
				.sources(BareConfiguration.class);
		this.sibling = builder.child(BareConfiguration.class)
				.properties("spring.application.name=sibling")
				.web(WebApplicationType.NONE).run();
		this.context = builder.child(BareConfiguration.class)
				.properties("spring.application.name=context")
				.web(WebApplicationType.NONE).run();
		then(TestHigherPriorityBootstrapConfiguration.count.get()).isEqualTo(1);
		then(this.context.getParent()).isNotNull();
		then(this.context.getParent().getParent().getId()).isEqualTo("bootstrap");
		then(this.context.getParent().getParent().getParent()).isNull();
		then(this.context.getEnvironment().getProperty("custom.foo"))
				.isEqualTo("context");
		then(this.context.getEnvironment().getProperty("spring.application.name"))
				.isEqualTo("context");
		then(this.sibling.getParent()).isNotNull();
		then(this.sibling.getParent().getParent().getId()).isEqualTo("bootstrap");
		then(this.sibling.getParent().getParent().getParent()).isNull();
		then(this.sibling.getEnvironment().getProperty("custom.foo"))
				.isEqualTo("sibling");
		then(this.sibling.getEnvironment().getProperty("spring.application.name"))
				.isEqualTo("sibling");
	}

	@Test
	public void environmentEnrichedInParentContext() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		then(this.context.getParent().getEnvironment())
				.isNotSameAs(this.context.getEnvironment());
		then(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
						+ "-testBootstrap")).isTrue();
		then(((ConfigurableEnvironment) this.context.getParent().getEnvironment())
				.getPropertySources().contains(
						PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
								+ "-testBootstrap")).isTrue();
	}

	@Test
	public void differentProfileInChild() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		// Profiles are always merged with the child
		ConfigurableApplicationContext parent = new SpringApplicationBuilder()
				.sources(BareConfiguration.class).profiles("parent")
				.web(WebApplicationType.NONE).run();
		this.context = new SpringApplicationBuilder(BareConfiguration.class)
				.profiles("child").parent(parent).web(WebApplicationType.NONE).run();
		then(this.context.getParent().getEnvironment())
				.isNotSameAs(this.context.getEnvironment());
		// The ApplicationContext merges profiles (profiles and property sources), see
		// AbstractEnvironment.merge()
		then(this.context.getEnvironment().acceptsProfiles("child", "parent")).isTrue();
		// But the parent is not a child
		then(this.context.getParent().getEnvironment().acceptsProfiles("child"))
				.isFalse();
		then(this.context.getParent().getEnvironment().acceptsProfiles("parent"))
				.isTrue();
		then(((ConfigurableEnvironment) this.context.getParent().getEnvironment())
				.getPropertySources().contains(
						PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME
								+ "-testBootstrap")).isTrue();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		// The "bootstrap" property source is not shared now, but it has the same
		// properties in it because they are pulled from the PropertySourceConfiguration
		// below
		then(this.context.getParent().getEnvironment().getProperty("bootstrap.foo"))
				.isEqualTo("bar");
		// The parent property source is there in the child because they are both in the
		// "parent" profile (by virtue of the merge in AbstractEnvironment)
		then(this.context.getEnvironment().getProperty("info.name")).isEqualTo("parent");
	}

	@Test
	public void includeProfileFromBootstrapPropertySource() {
		PropertySourceConfiguration.MAP.put("spring.profiles.include", "bar,baz");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.profiles("foo").sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().acceptsProfiles("baz")).isTrue();
		then(this.context.getEnvironment().acceptsProfiles("bar")).isTrue();
	}

	@Test
	public void includeProfileFromBootstrapProperties() {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class)
				.properties("spring.cloud.bootstrap.name=local").run();
		then(this.context.getEnvironment().acceptsProfiles("local")).isTrue();
		then(this.context.getEnvironment().getProperty("added"))
				.isEqualTo("Hello added!");
	}

	@Test
	public void nonEnumerablePropertySourceWorks() {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class)
				.properties("spring.cloud.bootstrap.name=nonenumerable").run();
		then(this.context.getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	protected static class BareConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class SimplePropertySourceConfiguration
			implements PropertySourceLocator {

		@Override
		public PropertySource<?> locate(Environment environment) {
			return new PropertySource("testBootstrapSimple", this) {
				@Override
				public Object getProperty(String name) {
					return ("foo".equals(name)) ? "bar" : null;
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConfigurationProperties("expected")
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class PropertySourceConfiguration implements PropertySourceLocator {

		public static Map<String, Object> MAP = new HashMap<String, Object>(
				Collections.<String, Object>singletonMap("bootstrap.foo", "bar"));

		private String name;

		private boolean fail = false;

		@Override
		public PropertySource<?> locate(Environment environment) {
			if (this.name != null) {
				then(this.name)
						.isEqualTo(environment.getProperty("spring.application.name"));
			}
			if (this.fail) {
				throw new RuntimeException("Planned");
			}
			return new MapPropertySource("testBootstrap", MAP);
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isFail() {
			return this.fail;
		}

		public void setFail(boolean fail) {
			this.fail = fail;
		}

	}

	@Configuration
	@ConfigurationProperties("compositeexpected")
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class CompositePropertySourceConfiguration
			implements PropertySourceLocator {

		public static Map<String, Object> MAP1 = new HashMap<String, Object>();

		public static Map<String, Object> MAP2 = new HashMap<String, Object>();

		public CompositePropertySourceConfiguration() {
			MAP1.put("list.foo[0]", "hello");
			MAP1.put("list.food[1]", "world");
			MAP2.put("list.foo[0]", "hello world");
		}

		private String name;

		private boolean fail = false;

		@Override
		public PropertySource<?> locate(Environment environment) {
			if (this.name != null) {
				then(this.name)
						.isEqualTo(environment.getProperty("spring.application.name"));
			}
			if (this.fail) {
				throw new RuntimeException("Planned");
			}
			CompositePropertySource compositePropertySource = new CompositePropertySource(
					"listTestBootstrap");
			compositePropertySource.addFirstPropertySource(
					new MapPropertySource("testBootstrap1", MAP1));
			compositePropertySource.addFirstPropertySource(
					new MapPropertySource("testBootstrap2", MAP2));
			return compositePropertySource;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isFail() {
			return this.fail;
		}

		public void setFail(boolean fail) {
			this.fail = fail;
		}

	}

	protected static class ListProperties {

		private List<String> foo;

		public List<String> getFoo() {
			return foo;
		}

		public void setFoo(List<String> foo) {
			this.foo = foo;
		}

	}

}
