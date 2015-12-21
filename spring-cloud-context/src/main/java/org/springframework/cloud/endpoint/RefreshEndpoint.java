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

package org.springframework.cloud.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties(prefix = "endpoints.refresh", ignoreUnknownFields = false)
@ManagedResource
public class RefreshEndpoint extends AbstractEndpoint<Collection<String>> {

	private Set<String> standardSources = new HashSet<String>(
			Arrays.asList(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));

	private ConfigurableApplicationContext context;

	private RefreshScope scope;

	public RefreshEndpoint(ConfigurableApplicationContext context, RefreshScope scope) {
		super("refresh");
		this.context = context;
		this.scope = scope;
	}

	@ManagedOperation
	public synchronized String[] refresh() {
		Map<String, Object> before = extract(
				this.context.getEnvironment().getPropertySources());
		addConfigFilesToEnvironment();
		Set<String> keys = changes(before,
				extract(this.context.getEnvironment().getPropertySources())).keySet();
		this.context.publishEvent(new EnvironmentChangeEvent(keys));
		this.scope.refreshAll();
		return keys.toArray(new String[keys.size()]);
	}

	private void addConfigFilesToEnvironment() {
		ConfigurableApplicationContext capture = null;
		try {
			StandardEnvironment environment = copyEnvironment(
					this.context.getEnvironment());
			capture = new SpringApplicationBuilder(Empty.class).bannerMode(Mode.OFF)
					.web(false).environment(environment).run();
			MutablePropertySources target = this.context.getEnvironment()
					.getPropertySources();
			String targetName = null;
			for (PropertySource<?> source : environment.getPropertySources()) {
				String name = source.getName();
				if (target.contains(name)) {
					targetName = name;
				}
				if (!this.standardSources.contains(name)) {
					if (target.contains(name)) {
						target.replace(name, source);
					}
					else {
						if (targetName != null) {
							target.addAfter(targetName, source);
						}
						else {
							if (target.contains("defaultProperties")) {
								target.addBefore("defaultProperties", source);
							}
							else {
								target.addLast(source);
							}
						}
					}
				}
			}
		}
		finally {
			ConfigurableApplicationContext closeable = capture;
			while (closeable != null) {
				closeable.close();
				ApplicationContext parent = closeable.getParent();
				if (parent instanceof ConfigurableApplicationContext) {
					closeable = (ConfigurableApplicationContext) parent;
				}
				else {
					closeable = null;
				}
			}
		}
	}

	// Don't use ConfigurableEnvironment.merge() in case there are clashes with property
	// source names
	private StandardEnvironment copyEnvironment(ConfigurableEnvironment input) {
		StandardEnvironment environment = new StandardEnvironment();
		MutablePropertySources capturedPropertySources = environment.getPropertySources();
		for (PropertySource<?> source : capturedPropertySources) {
			capturedPropertySources.remove(source.getName());
		}
		for (PropertySource<?> source : input.getPropertySources()) {
			capturedPropertySources.addLast(source);
		}
		environment.setActiveProfiles(input.getActiveProfiles());
		environment.setDefaultProfiles(input.getDefaultProfiles());
		return environment;
	}

	@Override
	public Collection<String> invoke() {
		return Arrays.asList(refresh());
	}

	private Map<String, Object> changes(Map<String, Object> before,
			Map<String, Object> after) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : before.keySet()) {
			if (!after.containsKey(key)) {
				result.put(key, null);
			}
			else if (!equal(before.get(key), after.get(key))) {
				result.put(key, after.get(key));
			}
		}
		for (String key : after.keySet()) {
			if (!before.containsKey(key)) {
				result.put(key, after.get(key));
			}
		}
		return result;
	}

	private boolean equal(Object one, Object two) {
		if (one == null && two == null) {
			return true;
		}
		if (one == null || two == null) {
			return false;
		}
		return one.equals(two);
	}

	private Map<String, Object> extract(MutablePropertySources propertySources) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
		for (PropertySource<?> source : propertySources) {
			sources.add(0, source);
		}
		for (PropertySource<?> source : sources) {
			if (!this.standardSources.contains(source.getName())) {
				extract(source, result);
			}
		}
		return result;
	}

	private void extract(PropertySource<?> parent, Map<String, Object> result) {
		if (parent instanceof CompositePropertySource) {
			try {
				List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
				for (PropertySource<?> source : ((CompositePropertySource) parent)
						.getPropertySources()) {
					sources.add(0, source);
				}
				for (PropertySource<?> source : sources) {
					extract(source, result);
				}
			}
			catch (Exception e) {
				return;
			}
		}
		else if (parent instanceof EnumerablePropertySource) {
			for (String key : ((EnumerablePropertySource<?>) parent).getPropertyNames()) {
				result.put(key, parent.getProperty(key));
			}
		}
	}

	@Configuration
	protected static class Empty {

	}

}
