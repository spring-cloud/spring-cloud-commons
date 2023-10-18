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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.logging.LoggingRebinder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.bootstrap.encrypt.AbstractEnvironmentDecrypt.DECRYPTED_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

/**
 * @author Dave Syer
 *
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropertySourceBootstrapProperties.class)
public class PropertySourceBootstrapConfiguration implements ApplicationListener<ContextRefreshedEvent>,
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	/**
	 * Bootstrap property source name.
	 */
	public static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME
			+ "Properties";

	private static Log logger = LogFactory.getLog(PropertySourceBootstrapConfiguration.class);

	private int order = Ordered.HIGHEST_PRECEDENCE + 10;

	@Autowired(required = false)
	private List<PropertySourceLocator> propertySourceLocators = new ArrayList<>();

	@Autowired
	private PropertySourceBootstrapProperties bootstrapProperties;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setPropertySourceLocators(Collection<PropertySourceLocator> propertySourceLocators) {
		this.propertySourceLocators = new ArrayList<>(propertySourceLocators);
	}

	/*
	 * The ApplicationListener is called when the main application context is initialized.
	 * This will be called after the ApplicationListener ContextRefreshedEvent is fired
	 * during the bootstrap phase. This method is also what added PropertySources prior to
	 * Spring Cloud 2021.0.7, this is why it will be called when
	 * spring.cloud.config.initialize-on-context-refresh is false. When
	 * spring.cloud.config.initialize-on-context-refresh is true this method provides a
	 * "second fetch" of configuration data to fetch any additional configuration data
	 * from profiles that have been activated.
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (!bootstrapProperties.isInitializeOnContextRefresh() || !applicationContext.getEnvironment()
				.getPropertySources().contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
			doInitialize(applicationContext);
		}
	}

	private void doInitialize(ConfigurableApplicationContext applicationContext) {
		List<PropertySource<?>> composite = new ArrayList<>();
		AnnotationAwareOrderComparator.sort(this.propertySourceLocators);
		boolean empty = true;
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		for (PropertySourceLocator locator : this.propertySourceLocators) {
			Collection<PropertySource<?>> source = locator.locateCollection(environment);
			if (source == null || source.size() == 0) {
				continue;
			}
			List<PropertySource<?>> sourceList = new ArrayList<>();
			for (PropertySource<?> p : source) {
				if (p instanceof EnumerablePropertySource) {
					EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) p;
					sourceList.add(new BootstrapPropertySource<>(enumerable));
				}
				else {
					sourceList.add(new SimpleBootstrapPropertySource(p));
				}
			}
			logger.info("Located property source: " + sourceList);
			composite.addAll(sourceList);
			empty = false;
		}
		if (!empty) {
			MutablePropertySources propertySources = environment.getPropertySources();
			String logConfig = environment.resolvePlaceholders("${logging.config:}");
			LogFile logFile = LogFile.get(environment);
			for (PropertySource<?> p : environment.getPropertySources()) {
				if (p.getName().startsWith(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
					propertySources.remove(p.getName());
				}
			}
			insertPropertySources(propertySources, composite);
			reinitializeLoggingSystem(environment, logConfig, logFile);
			setLogLevels(applicationContext, environment);
			handleProfiles(environment);
		}
	}

	private void reinitializeLoggingSystem(ConfigurableEnvironment environment, String oldLogConfig,
			LogFile oldLogFile) {
		Map<String, Object> props = Binder.get(environment).bind("logging", Bindable.mapOf(String.class, Object.class))
				.orElseGet(Collections::emptyMap);
		if (!props.isEmpty()) {
			String logConfig = environment.resolvePlaceholders("${logging.config:}");
			LogFile logFile = LogFile.get(environment);
			LoggingSystem system = LoggingSystem.get(LoggingSystem.class.getClassLoader());
			try {
				// Three step initialization that accounts for the clean up of the logging
				// context before initialization. Spring Boot doesn't initialize a logging
				// system that hasn't had this sequence applied (since 1.4.1).
				system.cleanUp();
				system.beforeInitialize();
				system.initialize(new LoggingInitializationContext(environment), logConfig, logFile);
			}
			catch (Exception ex) {
				PropertySourceBootstrapConfiguration.logger.warn("Error opening logging config file " + logConfig, ex);
			}
		}
	}

	private void setLogLevels(ConfigurableApplicationContext applicationContext, ConfigurableEnvironment environment) {
		LoggingRebinder rebinder = new LoggingRebinder();
		rebinder.setEnvironment(environment);
		// We can't fire the event in the ApplicationContext here (too early), but we can
		// create our own listener and poke it (it doesn't need the key changes)
		rebinder.onApplicationEvent(new EnvironmentChangeEvent(applicationContext, Collections.<String>emptySet()));
	}

	private void insertPropertySources(MutablePropertySources propertySources, List<PropertySource<?>> composite) {
		MutablePropertySources incoming = new MutablePropertySources();
		List<PropertySource<?>> reversedComposite = new ArrayList<>(composite);
		// Reverse the list so that when we call addFirst below we are maintaining the
		// same order of PropertySources
		// Wherever we call addLast we can use the order in the List since the first item
		// will end up before the rest
		Collections.reverse(reversedComposite);
		for (PropertySource<?> p : reversedComposite) {
			incoming.addFirst(p);
		}
		PropertySourceBootstrapProperties remoteProperties = new PropertySourceBootstrapProperties();
		Binder.get(environment(incoming)).bind("spring.cloud.config", Bindable.ofInstance(remoteProperties));
		if (!remoteProperties.isAllowOverride()
				|| (!remoteProperties.isOverrideNone() && remoteProperties.isOverrideSystemProperties())) {
			for (PropertySource<?> p : reversedComposite) {
				if (propertySources.contains(DECRYPTED_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(DECRYPTED_PROPERTY_SOURCE_NAME, p);
				}
				else {
					propertySources.addFirst(p);
				}
			}
			return;
		}
		if (remoteProperties.isOverrideNone()) {
			for (PropertySource<?> p : composite) {
				propertySources.addLast(p);
			}
			return;
		}
		if (propertySources.contains(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
			if (!remoteProperties.isOverrideSystemProperties()) {
				for (PropertySource<?> p : reversedComposite) {
					propertySources.addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, p);
				}
			}
			else {
				for (PropertySource<?> p : composite) {
					propertySources.addBefore(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, p);
				}
			}
		}
		else {
			for (PropertySource<?> p : composite) {
				propertySources.addLast(p);
			}
		}
	}

	private Environment environment(MutablePropertySources incoming) {
		ConfigurableEnvironment environment = new AbstractEnvironment() {
		};
		for (PropertySource<?> source : incoming) {
			environment.getPropertySources().addLast(source);
		}
		return environment;
	}

	private void handleProfiles(ConfigurableEnvironment environment) {
		if (bootstrapProperties.isInitializeOnContextRefresh() && !environment.getPropertySources()
				.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
			// In the case that spring.cloud.config.initialize-on-context-refresh is true
			// this method will
			// be called during the bootstrap phase and the main application startup. We
			// only manipulate the environment profiles in the bootstrap phase as we are
			// fetching
			// any additional profile specific configuration when this method would be
			// called during the
			// main application startup, and it is not valid to activate profiles in
			// profile specific
			// configuration properties, so we should not run this method then.
			return;
		}
		Set<String> includeProfiles = new TreeSet<>();
		List<String> activeProfiles = new ArrayList<>();

		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			addIncludedProfilesTo(includeProfiles, propertySource, environment);
			addActiveProfilesTo(activeProfiles, propertySource, environment);
		}

		// If it's already accepted we assume the order was set intentionally
		includeProfiles.removeAll(activeProfiles);
		// Prepend each added profile (last wins in a property key clash)
		for (String profile : includeProfiles) {
			activeProfiles.add(0, profile);
		}
		List<String> activeProfilesFromEnvironment = Arrays.stream(environment.getActiveProfiles())
				.collect(Collectors.toList());
		if (!activeProfiles.containsAll(activeProfilesFromEnvironment)) {
			activeProfiles.addAll(activeProfilesFromEnvironment);

		}
		environment.setActiveProfiles(activeProfiles.toArray(new String[activeProfiles.size()]));
	}

	private Set<String> addIncludedProfilesTo(Set<String> profiles, PropertySource<?> propertySource,
			ConfigurableEnvironment environment) {
		return addProfilesTo(profiles, propertySource, Profiles.INCLUDE_PROFILES_PROPERTY_NAME, environment);
	}

	private List<String> addActiveProfilesTo(List<String> profiles, PropertySource<?> propertySource,
			ConfigurableEnvironment environment) {
		// According to Spring Boot, "spring.profiles.active" should have priority,
		// only value from property source with the highest priority wins.
		// Once settled, ignore others
		if (!profiles.isEmpty()) {
			return profiles;
		}
		return addProfilesTo(profiles, propertySource, AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, environment);
	}

	private <T extends Collection<String>> T addProfilesTo(T profiles, PropertySource<?> propertySource,
			String property, ConfigurableEnvironment environment) {
		if (propertySource instanceof CompositePropertySource) {
			for (PropertySource<?> nestedPropertySource : ((CompositePropertySource) propertySource)
					.getPropertySources()) {
				addProfilesTo(profiles, nestedPropertySource, property, environment);
			}
		}
		else {
			Collections.addAll(profiles, getProfilesForValue(propertySource.getProperty(property), environment));
		}
		return profiles;
	}

	private String[] getProfilesForValue(Object property, ConfigurableEnvironment environment) {
		final String value = (property == null ? null : property.toString());
		return property == null ? new String[0] : resolvePlaceholdersInProfiles(value, environment);
	}

	private String[] resolvePlaceholdersInProfiles(String profiles, ConfigurableEnvironment environment) {
		return Arrays.stream(StringUtils.tokenizeToStringArray(profiles, ",")).map(s -> {
			if (s.startsWith("${") && s.endsWith("}")) {
				return environment.resolvePlaceholders(s);
			}
			else {
				return s;
			}
		}).toArray(String[]::new);
	}

	/*
	 * The ConextRefreshedEvent gets called at the end of the boostrap phase after config
	 * data is loaded during bootstrap. This will run and do an "initial fetch" of
	 * configuration data during bootstrap but before the main applicaiton context starts.
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (bootstrapProperties.isInitializeOnContextRefresh()
				&& event.getApplicationContext() instanceof ConfigurableApplicationContext) {
			if (((ConfigurableApplicationContext) event.getApplicationContext()).getEnvironment().getPropertySources()
					.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
				doInitialize((ConfigurableApplicationContext) event.getApplicationContext());
			}
		}
	}

}
