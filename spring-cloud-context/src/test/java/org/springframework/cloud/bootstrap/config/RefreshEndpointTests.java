/*
 * Copyright 2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 *
 */
public class RefreshEndpointTests {

	@Test
	public void shutdownHooksCleaned() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(Empty.class)
				.web(false).showBanner(false).run();
		RefreshScope scope = new RefreshScope();
		scope.setApplicationContext(context);
		RefreshEndpoint endpoint = new RefreshEndpoint(context, scope);
		int count = countShutdownHooks();
		endpoint.invoke();
		int after = countShutdownHooks();
		assertEquals("Shutdown hooks not cleaned on refresh", count, after);
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

	@Configuration
	protected static class Empty {

	}
}
