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

package org.springframework.cloud.endpoint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 * @author Venil Noronha
 */
public class RefreshEndpointTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void keysComputedWhenAdded() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class)
				.web(WebApplicationType.NONE).bannerMode(Mode.OFF)
				.properties("spring.cloud.bootstrap.name:none").run();
		RefreshScope scope = new RefreshScope();
		scope.setApplicationContext(this.context);
		this.context.getEnvironment().setActiveProfiles("local");
		ContextRefresher contextRefresher = new ContextRefresher(this.context, scope);
		RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
		Collection<String> keys = endpoint.refresh();
		then(keys.contains("added")).isTrue().as("Wrong keys: " + keys);
	}

	@Test
	public void keysComputedWhenOveridden() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class)
				.web(WebApplicationType.NONE).bannerMode(Mode.OFF)
				.properties("spring.cloud.bootstrap.name:none").run();
		RefreshScope scope = new RefreshScope();
		scope.setApplicationContext(this.context);
		this.context.getEnvironment().setActiveProfiles("override");
		ContextRefresher contextRefresher = new ContextRefresher(this.context, scope);
		RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
		Collection<String> keys = endpoint.refresh();
		then(keys.contains("message")).isTrue().as("Wrong keys: " + keys);
	}

	@Test
	public void keysComputedWhenChangesInExternalProperties() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class)
				.web(WebApplicationType.NONE).bannerMode(Mode.OFF)
				.properties("spring.cloud.bootstrap.name:none").run();
		RefreshScope scope = new RefreshScope();
		scope.setApplicationContext(this.context);
		TestPropertyValues
				.of("spring.cloud.bootstrap.sources="
						+ ExternalPropertySourceLocator.class.getName())
				.applyTo(this.context.getEnvironment(), Type.MAP, "defaultProperties");
		ContextRefresher contextRefresher = new ContextRefresher(this.context, scope);
		RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
		Collection<String> keys = endpoint.refresh();
		then(keys.contains("external.message")).isTrue().as("Wrong keys: " + keys);
	}

	@Test
	public void springMainSourcesEmptyInRefreshCycle() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class)
				.web(WebApplicationType.NONE).bannerMode(Mode.OFF)
				.properties("spring.cloud.bootstrap.name:none").run();
		RefreshScope scope = new RefreshScope();
		scope.setApplicationContext(this.context);
		// spring.main.sources should be empty when the refresh cycle starts (we don't
		// want any config files from the application context getting into the one used to
		// construct the environment for refresh)
		TestPropertyValues
				.of("spring.main.sources="
						+ ExternalPropertySourceLocator.class.getName())
				.applyTo(this.context);
		ContextRefresher contextRefresher = new ContextRefresher(this.context, scope);
		RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
		Collection<String> keys = endpoint.refresh();
		then(keys.contains("external.message")).as("Wrong keys: " + keys).isFalse();
	}

	@Test
	public void eventsPublishedInOrder() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class)
				.web(WebApplicationType.NONE).bannerMode(Mode.OFF).run();
		RefreshScope scope = new RefreshScope();
		scope.setApplicationContext(this.context);
		ContextRefresher contextRefresher = new ContextRefresher(this.context, scope);
		RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
		Empty empty = this.context.getBean(Empty.class);
		endpoint.refresh();
		int after = empty.events.size();
		then(2).isEqualTo(after).as("Shutdown hooks not cleaned on refresh");
		then(empty.events.get(0) instanceof EnvironmentChangeEvent).isTrue();
	}

	@Test
	public void shutdownHooksCleaned() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				Empty.class).web(WebApplicationType.NONE).bannerMode(Mode.OFF).run()) {
			RefreshScope scope = new RefreshScope();
			scope.setApplicationContext(context);
			ContextRefresher contextRefresher = new ContextRefresher(context, scope);
			RefreshEndpoint endpoint = new RefreshEndpoint(contextRefresher);
			int count = countShutdownHooks();
			endpoint.refresh();
			int after = countShutdownHooks();
			then(count).isEqualTo(after).as("Shutdown hooks not cleaned on refresh");
		}
	}

	private int countShutdownHooks() {
		Class<?> type = ClassUtils.resolveClassName("java.lang.ApplicationShutdownHooks",
				null);
		Field field = ReflectionUtils.findField(type, "hooks");
		ReflectionUtils.makeAccessible(field);
		@SuppressWarnings("rawtypes")
		Map map = (Map) ReflectionUtils.getField(field, null);
		return map.size();
	}

	@Configuration(proxyBeanMethods = false)
	protected static class Empty implements SmartApplicationListener {

		private List<ApplicationEvent> events = new ArrayList<ApplicationEvent>();

		@Override
		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
			return EnvironmentChangeEvent.class.isAssignableFrom(eventType)
					|| RefreshScopeRefreshedEvent.class.isAssignableFrom(eventType);
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof EnvironmentChangeEvent
					|| event instanceof RefreshScopeRefreshedEvent) {
				this.events.add(event);
			}
		}

	}

	@Component
	protected static class ExternalPropertySourceLocator
			implements PropertySourceLocator {

		@Override
		public PropertySource<?> locate(Environment environment) {
			return new MapPropertySource("external", Collections
					.<String, Object>singletonMap("external.message", "I'm External"));
		}

	}

}
