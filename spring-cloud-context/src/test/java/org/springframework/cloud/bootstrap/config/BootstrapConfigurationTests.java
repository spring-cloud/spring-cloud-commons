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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
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

	private ConfigurableApplicationContext context;

	private ConfigurableApplicationContext sibling;

	@AfterEach
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
		pickupOnlyExternalBootstrapProperties("spring.cloud.bootstrap.location=" + externalPropertiesPath,
				"spring.config.use-legacy-processing=true");
	}

	@Test
	public void pickupOnlyExternalBootstrapPropertiesWithAppListener() {
		String externalPropertiesPath = getExternalProperties();
		pickupOnlyExternalBootstrapProperties("spring.cloud.bootstrap.location=" + externalPropertiesPath,
				"spring.config.use-legacy-processing=true", "spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void pickupOnlyExternalBootstrapProperties(String... properties) {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(BareConfiguration.class)
				.properties(properties).run();
		then(this.context.getEnvironment().getProperty("info.name")).isEqualTo("externalPropertiesInfoName");
		then(this.context.getEnvironment().getProperty("info.desc")).isNull();
		then(this.context.getEnvironment().getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
	}

	@Test
	public void pickupAdditionalExternalBootstrapProperties() {
		String externalPropertiesPath = getExternalProperties();
		pickupAdditionalExternalBootstrapProperties(
				"spring.cloud.bootstrap.additional-location=" + externalPropertiesPath,
				"spring.config.use-legacy-processing=true");
	}

	@Test
	public void pickupAdditionalExternalBootstrapPropertiesWithAppListener() {
		String externalPropertiesPath = getExternalProperties();
		pickupAdditionalExternalBootstrapProperties(
				"spring.cloud.bootstrap.additional-location=" + externalPropertiesPath,
				"spring.config.use-legacy-processing=true", "spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void pickupAdditionalExternalBootstrapProperties(String... properties) {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(BareConfiguration.class)
				.properties(properties).run();
		then(this.context.getEnvironment().getProperty("info.name")).isEqualTo("externalPropertiesInfoName");
		then(this.context.getEnvironment().getProperty("info.desc")).isEqualTo("defaultPropertiesInfoDesc");
		then(this.context.getEnvironment().getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
	}

	@Test
	public void bootstrapPropertiesAvailableInInitializer() {
		bootstrapPropertiesAvailableInInitializer("spring.config.use-legacy-processing=true");
	}

	@Test
	public void bootstrapPropertiesAvailableInInitializerWithAppContext() {
		bootstrapPropertiesAvailableInInitializer("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void bootstrapPropertiesAvailableInInitializer(String... properties) {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).initializers(applicationContext -> {
					// This property is defined in bootstrap.properties
					then(applicationContext.getEnvironment().getProperty("info.name")).isEqualTo("child");
				}).run();
		then(this.context.getEnvironment().getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
	}

	/**
	 * Running the test from maven will start from a different directory then starting it
	 * from intellij
	 * @return external properties resource location
	 */
	private String getExternalProperties() {
		String externalPropertiesPath = "classpath:external-properties/bootstrap.properties";
		return externalPropertiesPath;
	}

	@Test
	public void picksUpAdditionalPropertySource() {
		picksUpAdditionalPropertySource("spring.config.use-legacy-processing=true");
	}

	@Test
	public void picksUpAdditionalPropertySourceWithAppContext() {
		picksUpAdditionalPropertySource("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void picksUpAdditionalPropertySource(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		then(this.context.getEnvironment().getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
	}

	@Test
	public void failsOnPropertySource() {
		failsOnPropertySource("spring.config.use-legacy-processing=true");
	}

	@Test
	public void failsOnPropertySourceWithAppContext() {
		failsOnPropertySource("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void failsOnPropertySource(String... properties) {
		System.setProperty("expected.fail", "true");
		Throwable throwable = Assertions.assertThrows(RuntimeException.class, () -> {
			this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
					.sources(BareConfiguration.class).run();
		});
		then(throwable.getMessage().equals("Planned"));
	}

	@Test
	public void overrideSystemPropertySourceByDefault() {
		overrideSystemPropertySourceByDefault("spring.config.use-legacy-processing=true");

	}

	@Test
	public void overrideSystemPropertySourceByDefaultWithAppContext() {
		overrideSystemPropertySourceByDefault("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");

	}

	private void overrideSystemPropertySourceByDefault(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
	}

	@Test
	public void systemPropertyOverrideFalse() {
		systemPropertyOverrideFalse("spring.config.use-legacy-processing=true");
	}

	@Test
	public void systemPropertyOverrideFalseWithAppContext() {
		systemPropertyOverrideFalse("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void systemPropertyOverrideFalse(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.overrideSystemProperties", "false");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("system");
	}

	@Test
	public void systemPropertyOverrideWhenOverrideDisallowed() {
		systemPropertyOverrideWhenOverrideDisallowed("spring.config.use-legacy-processing=true");
	}

	@Test
	public void systemPropertyOverrideWhenOverrideDisallowedWithAppContext() {
		systemPropertyOverrideWhenOverrideDisallowed("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void systemPropertyOverrideWhenOverrideDisallowed(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.overrideSystemProperties", "false");
		// If spring.cloud.config.allowOverride=false is in the remote property sources
		// with sufficiently high priority it always wins. Admins can enforce it by adding
		// their own remote property source.
		PropertySourceConfiguration.MAP.put("spring.cloud.config.allowOverride", "false");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
	}

	@Test
	public void systemPropertyOverrideFalseWhenOverrideAllowed() {
		systemPropertyOverrideFalseWhenOverrideAllowed("spring.config.use-legacy-processing=true");
	}

	@Test
	public void systemPropertyOverrideFalseWhenOverrideAllowedWithAppContext() {
		systemPropertyOverrideFalseWhenOverrideAllowed("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void systemPropertyOverrideFalseWhenOverrideAllowed(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.overrideSystemProperties", "false");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.allowOverride", "true");
		System.setProperty("bootstrap.foo", "system");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("system");
	}

	@Test
	public void overrideAllWhenOverrideAllowed() {
		overrideAllWhenOverrideAllowed("spring.config.use-legacy-processing=true");
	}

	@Test
	public void overrideAllWhenOverrideAllowedWithAppContext() {
		overrideAllWhenOverrideAllowed("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void overrideAllWhenOverrideAllowed(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.overrideNone", "true");
		PropertySourceConfiguration.MAP.put("spring.cloud.config.allowOverride", "true");
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(
				new MapPropertySource("last", Collections.<String, Object>singletonMap("bootstrap.foo", "splat")));
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.environment(environment).sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("splat");
	}

	@Test
	public void applicationNameInBootstrapAndMain() {
		applicationNameInBootstrapAndMain("spring.cloud.bootstrap.name:other",
				"spring.config.use-legacy-processing=true", "spring.config.name:plain");
	}

	@Test
	public void applicationNameInBootstrapAndMainWithAppContext() {
		applicationNameInBootstrapAndMain("spring.cloud.bootstrap.name:other",
				"spring.config.use-legacy-processing=true", "spring.config.name:plain",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void applicationNameInBootstrapAndMain(String... properties) {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("spring.application.name")).isEqualTo("app");
		// The parent is called "main" because spring.application.name is specified in
		// other.properties (the bootstrap properties)
		then(this.context.getParent().getEnvironment().getProperty("spring.application.name")).isEqualTo("main");
		// The bootstrap context has the same "bootstrap" property source
		then(((ConfigurableEnvironment) this.context.getParent().getEnvironment()).getPropertySources()
				.get("bootstrap")).isEqualTo(this.context.getEnvironment().getPropertySources().get("bootstrap"));
		then(this.context.getId()).isEqualTo("main-1");
	}

	@Test
	public void applicationNameNotInBootstrap() {
		applicationNameNotInBootstrap("spring.cloud.bootstrap.name:application",
				"spring.config.use-legacy-processing=true", "spring.config.name:other");
	}

	@Test
	public void applicationNameNotInBootstrapWithAppContext() {
		applicationNameNotInBootstrap("spring.cloud.bootstrap.name:application",
				"spring.config.use-legacy-processing=true", "spring.config.name:other",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void applicationNameNotInBootstrap(String... properties) {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().getProperty("spring.application.name")).isEqualTo("main");
		// The parent has no name because spring.application.name is not
		// defined in the bootstrap properties
		then(this.context.getParent().getEnvironment().getProperty("spring.application.name")).isEqualTo(null);
	}

	@Test
	public void applicationNameOnlyInBootstrap() {
		applicationNameOnlyInBootstrap("spring.cloud.bootstrap.name:other", "spring.config.use-legacy-processing=true");
	}

	@Test
	public void applicationNameOnlyInBootstrapWithAppContext() {
		applicationNameOnlyInBootstrap("spring.cloud.bootstrap.name:other", "spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void applicationNameOnlyInBootstrap(String... properties) {
		System.setProperty("expected.name", "main");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run();
		// The main context is called "main" because spring.application.name is specified
		// in other.properties (and not in the main config file)
		then(this.context.getEnvironment().getProperty("spring.application.name")).isEqualTo("main");
		// The parent is called "main" because spring.application.name is specified in
		// other.properties (the bootstrap properties this time)
		then(this.context.getParent().getEnvironment().getProperty("spring.application.name")).isEqualTo("main");
		then(this.context.getId()).isEqualTo("main-1");
	}

	@Test
	public void environmentEnrichedOnceWhenSharedWithChildContext() {
		environmentEnrichedOnceWhenSharedWithChildContext("spring.config.use-legacy-processing=true");
	}

	@Test
	public void environmentEnrichedOnceWhenSharedWithChildContextWithAppContext() {
		environmentEnrichedOnceWhenSharedWithChildContext("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void environmentEnrichedOnceWhenSharedWithChildContext(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class).properties(properties)
				.environment(new StandardEnvironment()).child(BareConfiguration.class).web(WebApplicationType.NONE)
				.run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		then(this.context.getParent().getEnvironment()).isEqualTo(this.context.getEnvironment());
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		PropertySource<?> bootstrap = sources
				.get(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap");
		then(bootstrap).isNotNull();
		then(sources.precedenceOf(bootstrap)).isEqualTo(0);
	}

	@Test
	public void onlyOneBootstrapContext() {
		onlyOneBootstrapContext("spring.config.use-legacy-processing=true");
	}

	@Test
	public void onlyOneBootstrapContextWithAppContext() {
		onlyOneBootstrapContext("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void onlyOneBootstrapContext(String... properties) {
		TestHigherPriorityBootstrapConfiguration.count.set(0);
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class).properties(properties)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		then(TestHigherPriorityBootstrapConfiguration.count.get()).isEqualTo(1);
		then(this.context.getParent()).isNotNull();
		then(this.context.getParent().getParent().getId()).isEqualTo("bootstrap");
		then(this.context.getParent().getParent().getParent()).isNull();
		then(this.context.getEnvironment().getProperty("custom.foo")).isEqualTo("bar");
	}

	@Test
	public void listOverride() {
		listOverride("spring.config.use-legacy-processing=true");
	}

	@Test
	public void listOverrideWithAppContext() {
		listOverride("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void listOverride(String... properties) {
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class).properties(properties)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		ListProperties listProperties = new ListProperties();
		Binder.get(this.context.getEnvironment()).bind("list", Bindable.ofInstance(listProperties));
		then(listProperties.getFoo().size()).isEqualTo(1);
		then(listProperties.getFoo().get(0)).isEqualTo("hello world");
	}

	@Test
	public void bootstrapContextSharedBySiblings() {
		bootstrapContextSharedBySiblings("spring.config.use-legacy-processing=true");
	}

	@Test
	public void bootstrapContextSharedBySiblingsWithAppContext() {
		bootstrapContextSharedBySiblings("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void bootstrapContextSharedBySiblings(String... properties) {
		TestHigherPriorityBootstrapConfiguration.count.set(0);
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		SpringApplicationBuilder builder = new SpringApplicationBuilder().properties(properties)
				.sources(BareConfiguration.class);
		this.sibling = builder.child(BareConfiguration.class).properties("spring.application.name=sibling")
				.web(WebApplicationType.NONE).run();
		this.context = builder.child(BareConfiguration.class).properties("spring.application.name=context")
				.web(WebApplicationType.NONE).run();
		then(TestHigherPriorityBootstrapConfiguration.count.get()).isEqualTo(1);
		then(this.context.getParent()).isNotNull();
		then(this.context.getParent().getParent().getId()).isEqualTo("bootstrap");
		then(this.context.getParent().getParent().getParent()).isNull();
		then(this.context.getEnvironment().getProperty("custom.foo")).isEqualTo("context");
		then(this.context.getEnvironment().getProperty("spring.application.name")).isEqualTo("context");
		then(this.sibling.getParent()).isNotNull();
		then(this.sibling.getParent().getParent().getId()).isEqualTo("bootstrap");
		then(this.sibling.getParent().getParent().getParent()).isNull();
		then(this.sibling.getEnvironment().getProperty("custom.foo")).isEqualTo("sibling");
		then(this.sibling.getEnvironment().getProperty("spring.application.name")).isEqualTo("sibling");
	}

	@Test
	public void environmentEnrichedInParentContext() {
		environmentEnrichedInParentContext("spring.config.use-legacy-processing=true");
	}

	@Test
	public void environmentEnrichedInParentContextWithAppContext() {
		environmentEnrichedInParentContext("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void environmentEnrichedInParentContext(String... properties) {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		this.context = new SpringApplicationBuilder().sources(BareConfiguration.class).properties(properties)
				.child(BareConfiguration.class).web(WebApplicationType.NONE).run();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		then(this.context.getParent().getEnvironment()).isNotSameAs(this.context.getEnvironment());
		then(this.context.getEnvironment().getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
		then(((ConfigurableEnvironment) this.context.getParent().getEnvironment()).getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
	}

	@Test
	@Disabled // FIXME: legacy
	public void differentProfileInChild() {
		PropertySourceConfiguration.MAP.put("bootstrap.foo", "bar");
		// Profiles are always merged with the child
		ConfigurableApplicationContext parent = new SpringApplicationBuilder().sources(BareConfiguration.class)
				.profiles("parent").web(WebApplicationType.NONE).run();
		this.context = new SpringApplicationBuilder(BareConfiguration.class)
				.properties("spring.config.use-legacy-processing=true").profiles("child").parent(parent)
				.web(WebApplicationType.NONE).run();
		then(this.context.getParent().getEnvironment()).isNotSameAs(this.context.getEnvironment());
		// The ApplicationContext merges profiles (profiles and property sources), see
		// AbstractEnvironment.merge()
		then(this.context.getEnvironment().acceptsProfiles("child", "parent")).isTrue();
		// But the parent is not a child
		then(this.context.getParent().getEnvironment().acceptsProfiles("child")).isFalse();
		then(this.context.getParent().getEnvironment().acceptsProfiles("parent")).isTrue();
		then(((ConfigurableEnvironment) this.context.getParent().getEnvironment()).getPropertySources()
				.contains(PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME + "-testBootstrap"))
						.isTrue();
		then(this.context.getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		// The "bootstrap" property source is not shared now, but it has the same
		// properties in it because they are pulled from the PropertySourceConfiguration
		// below
		then(this.context.getParent().getEnvironment().getProperty("bootstrap.foo")).isEqualTo("bar");
		// The parent property source is there in the child because they are both in the
		// "parent" profile (by virtue of the merge in AbstractEnvironment)
		then(this.context.getEnvironment().getProperty("info.name")).isEqualTo("parent");
	}

	@Test
	public void includeProfileFromBootstrapPropertySource() {
		includeProfileFromBootstrapPropertySource("spring.config.use-legacy-processing=true");
	}

	@Test
	public void includeProfileFromBootstrapPropertySourceWithAppContext() {
		includeProfileFromBootstrapPropertySource("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void includeProfileFromBootstrapPropertySource(String... properties) {
		PropertySourceConfiguration.MAP.put("spring.profiles.include", "bar,baz");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.profiles("foo").sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().acceptsProfiles("baz")).isTrue();
		then(this.context.getEnvironment().acceptsProfiles("bar")).isTrue();
	}

	@Test
	public void activeProfileFromBootstrapPropertySource() {
		activeProfileFromBootstrapPropertySource("spring.config.use-legacy-processing=true");
	}

	@Test
	public void activeProfileFromBootstrapPropertySourceWithAppContext() {
		activeProfileFromBootstrapPropertySource("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
		then(this.context.getEnvironment().getActiveProfiles()).doesNotContain("after");
	}

	private void activeProfileFromBootstrapPropertySource(String... properties) {
		PropertySourceConfiguration.MAP.put("spring.profiles.active", "bar,baz");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.profiles("foo").sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().acceptsProfiles("baz", "bar", "foo")).isTrue();
		then(this.context.getEnvironment().getActiveProfiles()).contains("baz", "bar", "foo");
	}

	@Test
	public void activeAndIncludeProfileFromBootstrapPropertySource() {
		activeAndIncludeProfileFromBootstrapPropertySource("spring.config.use-legacy-processing=true");
	}

	@Test
	public void activeAndIncludeProfileFromBootstrapPropertySourceWithAppContext() {
		activeAndIncludeProfileFromBootstrapPropertySource("spring.config.use-legacy-processing=true",
				"spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void activeAndIncludeProfileFromBootstrapPropertySource(String... properties) {
		PropertySourceConfiguration.MAP.put("spring.profiles.active", "bar,baz");
		PropertySourceConfiguration.MAP.put("spring.profiles.include", "bar,baz,hello");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.profiles("foo").sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().acceptsProfiles("baz", "bar", "hello", "foo")).isTrue();
		then(this.context.getEnvironment().getActiveProfiles()).contains("baz", "bar", "foo", "hello");
	}

	@Test
	public void activeAndIncludeProfileFromBootstrapPropertySourceWithReplacement() {
		activeAndIncludeProfileFromBootstrapPropertySourceWithReplacement("spring.config.use-legacy-processing=true",
				"barreplacement=bar");
	}

	@Test
	public void activeAndIncludeProfileFromBootstrapPropertySourceWithReplacementWithAppContext() {
		activeAndIncludeProfileFromBootstrapPropertySourceWithReplacement("spring.config.use-legacy-processing=true",
				"barreplacement=bar", "spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void activeAndIncludeProfileFromBootstrapPropertySourceWithReplacement(String... properties) {
		PropertySourceConfiguration.MAP.put("spring.profiles.active", "${barreplacement},baz");
		PropertySourceConfiguration.MAP.put("spring.profiles.include", "${barreplacement},baz,hello");
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.profiles("foo").sources(BareConfiguration.class).run();
		then(this.context.getEnvironment().acceptsProfiles("baz", "bar", "hello", "foo")).isTrue();
		then(this.context.getEnvironment().getActiveProfiles()).contains("baz", "bar", "foo", "hello");
	}

	@Test
	public void includeProfileFromBootstrapProperties() {
		includeProfileFromBootstrapProperties("spring.config.use-legacy-processing=true",
				"spring.cloud.bootstrap.name=local");
	}

	@Test
	public void includeProfileFromBootstrapPropertiesWithAppContext() {
		includeProfileFromBootstrapProperties("spring.config.use-legacy-processing=true",
				"spring.cloud.bootstrap.name=local", "spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void includeProfileFromBootstrapProperties(String... properties) {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(BareConfiguration.class)
				.properties(properties).run();
		then(this.context.getEnvironment().acceptsProfiles("local")).isTrue();
		then(this.context.getEnvironment().getProperty("added")).isEqualTo("Hello added!");
	}

	@Test
	public void nonEnumerablePropertySourceWorks() {
		nonEnumerablePropertySourceWorks("spring.config.use-legacy-processing=true",
				"spring.cloud.bootstrap.name=nonenumerable");
	}

	@Test
	public void nonEnumerablePropertySourceWorksWithAppContext() {
		nonEnumerablePropertySourceWorks("spring.config.use-legacy-processing=true",
				"spring.cloud.bootstrap.name=nonenumerable", "spring.cloud.config.initialize-on-context-refresh=true");
	}

	private void nonEnumerablePropertySourceWorks(String... properties) {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(BareConfiguration.class)
				.properties(properties).run();
		then(this.context.getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void activeAndIncludeProfileFromBootstrapPropertySource_WhenMultiplePlacesHaveActiveProfileProperties_ShouldOnlyAcceptTheTopPriority() {
		String[] properties = new String[] { "spring.config.use-legacy-processing=true" };
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(BareConfiguration.class).run("--spring.profiles.active=prod,secure");
		then(this.context.getEnvironment().acceptsProfiles("prod", "secure")).isTrue();
		// active profile from property sources with lower priority should not be included
		then(this.context.getEnvironment().acceptsProfiles("local")).isFalse();
		then(this.context.getEnvironment().getActiveProfiles()).contains("prod", "secure");
		then(this.context.getEnvironment().getActiveProfiles()).doesNotContain("local");
		// check if active profile value could possibly exist in other property sources
		// with lower priority
		then(this.context.getEnvironment().getPropertySources().stream()
				.map(p -> p.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME)).anyMatch("local"::equals))
						.isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	protected static class BareConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class SimplePropertySourceConfiguration implements PropertySourceLocator {

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
			if (environment instanceof ConfigurableEnvironment) {
				if (!((ConfigurableEnvironment) environment).getPropertySources()
						.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
					if (MAP.containsKey(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME)) {
						// This additional profile, after, should not be added when
						// initialize-on-context-refresh=true
						MAP.put(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME,
								MAP.get(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME) + ",after");
					}
				}
			}
			if (this.name != null) {
				then(this.name).isEqualTo(environment.getProperty("spring.application.name"));
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
	protected static class CompositePropertySourceConfiguration implements PropertySourceLocator {

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
				then(this.name).isEqualTo(environment.getProperty("spring.application.name"));
			}
			if (this.fail) {
				throw new RuntimeException("Planned");
			}
			CompositePropertySource compositePropertySource = new CompositePropertySource("listTestBootstrap");
			compositePropertySource.addFirstPropertySource(new MapPropertySource("testBootstrap1", MAP1));
			compositePropertySource.addFirstPropertySource(new MapPropertySource("testBootstrap2", MAP2));
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
