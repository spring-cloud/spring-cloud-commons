/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.logging.LoggingRebinder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ResourceUtils;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties(PropertySourceBootstrapProperties.class)
public class PropertySourceBootstrapConfiguration implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	public static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME
			+ "Properties";

	private static Log logger = LogFactory
			.getLog(PropertySourceBootstrapConfiguration.class);

	private int order = Ordered.HIGHEST_PRECEDENCE + 10;

	@Autowired(required = false)
	private List<PropertySourceLocator> propertySourceLocators = new ArrayList<>();

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setPropertySourceLocators(
			Collection<PropertySourceLocator> propertySourceLocators) {
		this.propertySourceLocators = new ArrayList<>(propertySourceLocators);
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		CompositePropertySource composite = new CompositePropertySource(
				BOOTSTRAP_PROPERTY_SOURCE_NAME);
		AnnotationAwareOrderComparator.sort(this.propertySourceLocators);
		boolean empty = true;
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		for (PropertySourceLocator locator : this.propertySourceLocators) {
			PropertySource<?> source = null;
			source = locator.locate(environment);
			if (source == null) {
				continue;
			}
			logger.info("Located property source: " + source);
			composite.addPropertySource(source);
			empty = false;
		}
		if (!empty) {
			MutablePropertySources propertySources = environment.getPropertySources();
			String logConfig = environment.resolvePlaceholders("${logging.config:}");
			LogFile logFile = LogFile.get(environment);
			if (propertySources.contains(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
				propertySources.remove(BOOTSTRAP_PROPERTY_SOURCE_NAME);
			}
			insertPropertySources(propertySources, composite);
			reinitializeLoggingSystem(environment, logConfig, logFile);
			setLogLevels(environment);
		}
	}

	private void reinitializeLoggingSystem(ConfigurableEnvironment environment,
			String oldLogConfig, LogFile oldLogFile) {
		Map<String, Object> props = new RelaxedPropertyResolver(environment)
				.getSubProperties("logging.");
		if (!props.isEmpty()) {
			String logConfig = environment.resolvePlaceholders("${logging.config:}");
			LogFile logFile = LogFile.get(environment);
			LoggingSystem system = LoggingSystem
					.get(LoggingSystem.class.getClassLoader());
			try {
				ResourceUtils.getURL(logConfig).openStream().close();
				// Three step initialization that accounts for the clean up of the logging
				// context before initialization. Spring Boot doesn't initialize a logging
				// system that hasn't had this sequence applied (since 1.4.1).
				system.cleanUp();
				system.beforeInitialize();
				system.initialize(new LoggingInitializationContext(environment),
						logConfig, logFile);
			}
			catch (Exception ex) {
				PropertySourceBootstrapConfiguration.logger
						.warn("Logging config file location '" + logConfig
								+ "' cannot be opened and will be ignored");
			}
		}
	}

	private void setLogLevels(ConfigurableEnvironment environment) {
		LoggingRebinder rebinder = new LoggingRebinder();
		rebinder.setEnvironment(environment);
		// We can't fire the event in the ApplicationContext here (too early), but we can
		// create our own listener and poke it (it doesn't need the key changes)
		rebinder.onApplicationEvent(
				new EnvironmentChangeEvent(Collections.<String> emptySet()));
	}

	private void insertPropertySources(MutablePropertySources propertySources,
			CompositePropertySource composite) {
		MutablePropertySources incoming = new MutablePropertySources();
		incoming.addFirst(composite);
		PropertySourceBootstrapProperties remoteProperties = new PropertySourceBootstrapProperties();
		new RelaxedDataBinder(remoteProperties, "spring.cloud.config")
				.bind(new PropertySourcesPropertyValues(incoming));
		if (!remoteProperties.isAllowOverride() || (!remoteProperties.isOverrideNone()
				&& remoteProperties.isOverrideSystemProperties())) {
			propertySources.addFirst(composite);
			return;
		}
		if (remoteProperties.isOverrideNone()) {
			propertySources.addLast(composite);
			return;
		}
		if (propertySources
				.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
			if (!remoteProperties.isOverrideSystemProperties()) {
				propertySources.addAfter(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						composite);
			}
			else {
				propertySources.addBefore(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						composite);
			}
		}
		else {
			propertySources.addLast(composite);
		}
	}

}
