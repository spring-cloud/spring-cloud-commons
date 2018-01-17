/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 *
 */
public class BootstrapConfigurationTests {

	private ConfigurableApplicationContext context;

	private ConfigurableApplicationContext sibling;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@After
	public void close() {
		// Expected.* is bound to the PropertySourceConfiguration below
		System.clearProperty("expected.name");
		System.clearProperty("expected.fail");
		// Used to test system properties override
		System.clearProperty("bootstrap.foo");
		PropertySourceConfiguration.MAP.clear();
		if (this.context != null) {
			this.context.close();
		}
		if (this.sibling != null) {
			this.sibling.close();
		}
	}

	@Test
	public void pickupExternalBootstrapProperties() {
		String externalPropertiesPath = getExternalProperties();

		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class)
				.properties("spring.cloud.bootstrap.location=" + externalPropertiesPath)
				.run();
		assertEquals("externalPropertiesInfoName",
				this.context.getEnvironment().getProperty("info.name"));
		assertTrue(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
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
								assertEquals("child", applicationContext.getEnvironment()
										.getProperty("info.name"));
							}
						})
				.run();
		assertTrue(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
	}

	/**
	 * Running the test from maven will start from a different directory then starting it
	 * from intellij
	 *
	 * @return
	 */
	private String getExternalProperties() {
		String externalPropertiesPath = "classpath:bootstrap.properties,classpath:external-properties/bootstrap.properties";
		return externalPropertiesPath;
	}

	@Test
	public void picksUpAdditionalPropertySource() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		assertEquals("bar", this.context.getEnvironment().getProperty("bootstrap.foo"));
		assertTrue(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
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
		assertEquals("bar", this.context.getEnvironment().getProperty("bootstrap.foo"));
	}

	@Test
	public void systemPropertyOverrideFalse() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP
				.put("spring.cloud.config.overrideSystemProperties", "false");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class).run();
		assertEquals("system",
				this.context.getEnvironment().getProperty("bootstrap.foo"));
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
		assertEquals("bar", this.context.getEnvironment().getProperty("bootstrap.foo"));
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
		assertEquals("system",
				this.context.getEnvironment().getProperty("bootstrap.foo"));
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
		assertEquals("splat", this.context.getEnvironment().getProperty("bootstrap.foo"));
	}

	@Test
	public void applicationNameInBootstrapAndMain() {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.cloud.bootstrap.name:other",
						"spring.config.name:plain")
				.sources(BareConfiguration.class).run();
		assertEquals("app",
				this.context.getEnvironment().getProperty("spring.application.name"));
		// The parent is called "main" because spring.application.name is specified in
		// other.properties (the bootstrap properties)
		assertEquals("main", this.context.getParent().getEnvironment()
				.getProperty("spring.application.name"));
		// The bootstrap context has the same "bootstrap" property source
		assertEquals(this.context.getEnvironment().getPropertySources().get("bootstrap"),
				((ConfigurableEnvironment) this.context.getParent().getEnvironment())
						.getPropertySources().get("bootstrap"));
		assertEquals("main-1", this.context.getId());
	}

	@Test
	public void applicationNameNotInBootstrap() {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.cloud.bootstrap.name:application",
						"spring.config.name:other")
				.sources(BareConfiguration.class).run();
		assertEquals("main",
				this.context.getEnvironment().getProperty("spring.application.name"));
		// The parent has no name because spring.application.name is not
		// defined in the bootstrap properties
		assertEquals(null, this.context.getParent().getEnvironment()
				.getProperty("spring.application.name"));
	}

	@Test
	public void applicationNameOnlyInBootstrap() {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("spring.cloud.bootstrap.name:other")
				.sources(BareConfiguration.class).run();
		// The main context is called "main" because spring.application.name is specified
		// in other.properties (and not in the main config file)
		assertEquals("main",
				this.context.getEnvironment().getProperty("spring.application.name"));
		// The parent is called "main" because spring.application.name is specified in
		// other.properties (the bootstrap properties this time)
		assertEquals("main", this.context.getParent().getEnvironment()
				.getProperty("spring.application.name"));
		assertEquals("main-1", this.context.getId());
	}

	@Test
	public void environmentEnrichedOnceWhenSharedWithChildContext() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.environment(new StandardEnvironment()).child(BareConfiguration.class)
				.web(WebApplicationType.NONE).run();
		assertEquals("bar", this.context.getEnvironment().getProperty("bootstrap.foo"));
		assertEquals(this.context.getEnvironment(),
				this.context.getParent().getEnvironment());
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		PropertySource<?> bootstrap = sources
				.get(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME);
		assertNotNull(bootstrap);
		assertEquals(0, sources.precedenceOf(bootstrap));
	}

	@Test
	public void onlyOneBootstrapContext() {
		TestHigherPriorityBootstrapConfiguration.count.set(0);
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		assertEquals(1, TestHigherPriorityBootstrapConfiguration.count.get());
		assertNotNull(context.getParent());
		assertEquals("bootstrap", context.getParent().getParent().getId());
		assertNull(context.getParent().getParent().getParent());
		assertEquals("bar", context.getEnvironment().getProperty("custom.foo"));
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
		assertEquals(1, TestHigherPriorityBootstrapConfiguration.count.get());
		assertNotNull(context.getParent());
		assertEquals("bootstrap", context.getParent().getParent().getId());
		assertNull(context.getParent().getParent().getParent());
		assertEquals("context", context.getEnvironment().getProperty("custom.foo"));
		assertEquals("context",
				context.getEnvironment().getProperty("spring.application.name"));
		assertNotNull(sibling.getParent());
		assertEquals("bootstrap", sibling.getParent().getParent().getId());
		assertNull(sibling.getParent().getParent().getParent());
		assertEquals("sibling", sibling.getEnvironment().getProperty("custom.foo"));
		assertEquals("sibling",
				sibling.getEnvironment().getProperty("spring.application.name"));
	}

	@Test
	public void environmentEnrichedInParentContext() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		assertEquals("bar", this.context.getEnvironment().getProperty("bootstrap.foo"));
		assertNotSame(this.context.getEnvironment(),
				this.context.getParent().getEnvironment());
		assertTrue(this.context.getEnvironment().getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
		assertTrue(((ConfigurableEnvironment) this.context.getParent().getEnvironment())
				.getPropertySources().contains(
						PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
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
		assertNotSame(this.context.getEnvironment(),
				this.context.getParent().getEnvironment());
		// The ApplicationContext merges profiles (profiles and property sources), see
		// AbstractEnvironment.merge()
		assertTrue(this.context.getEnvironment().acceptsProfiles("child", "parent"));
		// But the parent is not a child
		assertFalse(this.context.getParent().getEnvironment().acceptsProfiles("child"));
		assertTrue(this.context.getParent().getEnvironment().acceptsProfiles("parent"));
		assertTrue(((ConfigurableEnvironment) this.context.getParent().getEnvironment())
				.getPropertySources().contains(
						PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
		assertEquals("bar", this.context.getEnvironment().getProperty("bootstrap.foo"));
		// The "bootstrap" property source is not shared now, but it has the same
		// properties in it because they are pulled from the PropertySourceConfiguration
		// below
		assertEquals("bar",
				this.context.getParent().getEnvironment().getProperty("bootstrap.foo"));
		// The parent property source is there in the child because they are both in the
		// "parent" profile (by virtue of the merge in AbstractEnvironment)
		assertEquals("parent", this.context.getEnvironment().getProperty("info.name"));
	}

	@Test
	public void includeProfileFromBootstrapPropertySource() {
		PropertySourceConfiguration.MAP.put("spring.profiles.include", "bar,baz");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.profiles("foo").sources(BareConfiguration.class).run();
		assertTrue(this.context.getEnvironment().acceptsProfiles("baz"));
		assertTrue(this.context.getEnvironment().acceptsProfiles("bar"));
	}

	@Test
	public void includeProfileFromBootstrapProperties() {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(BareConfiguration.class)
				.properties("spring.cloud.bootstrap.name=local").run();
		assertTrue(this.context.getEnvironment().acceptsProfiles("local"));
		assertEquals("Hello added!", this.context.getEnvironment().getProperty("added"));
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class BareConfiguration {
	}

	@Configuration
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
				assertEquals(this.name,
						environment.getProperty("spring.application.name"));
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

}
