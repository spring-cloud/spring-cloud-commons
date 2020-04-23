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

package org.springframework.cloud.autoconfigure;

import java.util.List;
import java.util.function.Function;

import org.assertj.core.util.Lists;
import org.junit.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 */
// TODO: super slow. Port to @SpringBootTest
public class LifecycleMvcAutoConfigurationTests {

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration, String... properties) {

		List<String> defaultProperties = Lists.newArrayList(properties);
		defaultProperties.add("server.port=0");
		defaultProperties.add("spring.jmx.default-domain=${random.uuid}");

		return new SpringApplicationBuilder(configuration)
				.properties(defaultProperties.toArray(new String[] {})).run();
	}

	@Test
	public void environmentWebEndpointExtensionDisabled() {
		beanNotCreated("writableEnvironmentEndpointWebExtension",
				"management.endpoint.env.enabled=false");
	}

	@Test
	public void environmentWebEndpointExtensionGloballyDisabled() {
		beanNotCreated("writableEnvironmentEndpointWebExtension",
				"management.endpoints.enabled-by-default=false");
	}

	@Test
	public void environmentWebEndpointExtensionEnabled() {
		beanCreated("writableEnvironmentEndpointWebExtension",
				"management.endpoint.env.enabled=true",
				"management.endpoint.env.post.enabled=true",
				"management.endpoints.web.exposure.include=env");
	}

	// restartEndpoint
	@Test
	public void restartEndpointDisabled() {
		beanNotCreated("restartEndpoint", "management.endpoint.restart.enabled=false");
	}

	@Test
	public void restartEndpointGloballyDisabled() {
		beanNotCreated("restartEndpoint", "management.endpoint.default.enabled=false");
	}

	@Test
	public void restartEndpointEnabled() {
		beanCreatedAndEndpointEnabled("restartEndpoint", RestartEndpoint.class,
				RestartEndpoint::restart, "management.endpoint.restart.enabled=true",
				"management.endpoints.web.exposure.include=restart");
	}

	// pauseEndpoint
	@Test
	public void pauseEndpointDisabled() {
		beanNotCreated("pauseEndpoint", "management.endpoint.pause.enabled=false");
	}

	@Test
	public void pauseEndpointRestartDisabled() {
		beanNotCreated("pauseEndpoint", "management.endpoint.restart.enabled=false",
				"management.endpoint.pause.enabled=true");
	}

	@Test
	public void pauseEndpointGloballyDisabled() {
		beanNotCreated("pauseEndpoint", "management.endpoint.default.enabled=false");
	}

	@Test
	public void pauseEndpointEnabled() {
		beanCreatedAndEndpointEnabled("pauseEndpoint",
				RestartEndpoint.PauseEndpoint.class, RestartEndpoint.PauseEndpoint::pause,
				"management.endpoint.restart.enabled=true",
				"management.endpoints.web.exposure.include=restart,pause",
				"management.endpoint.pause.enabled=true");
	}

	// resumeEndpoint
	@Test
	public void resumeEndpointDisabled() {
		beanNotCreated("resumeEndpoint", "management.endpoint.restart.enabled=true",
				"management.endpoints.web.exposure.include=restart",
				"management.endpoint.resume.enabled=false");
	}

	@Test
	public void resumeEndpointRestartDisabled() {
		beanNotCreated("resumeEndpoint", "management.endpoint.restart.enabled=false",
				"management.endpoints.web.exposure.include=resume",
				"management.endpoint.resume.enabled=true");
	}

	@Test
	public void resumeEndpointGloballyDisabled() {
		beanNotCreated("resumeEndpoint", "management.endpoint.default.enabled=false");
	}

	@Test
	public void resumeEndpointEnabled() {
		beanCreatedAndEndpointEnabled("resumeEndpoint",
				RestartEndpoint.ResumeEndpoint.class,
				RestartEndpoint.ResumeEndpoint::resume,
				"management.endpoint.restart.enabled=true",
				"management.endpoint.resume.enabled=true",
				"management.endpoints.web.exposure.include=restart,resume");
	}

	private void beanNotCreated(String beanName, String... contextProperties) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class,
				contextProperties)) {
			then(context.containsBeanDefinition(beanName))
					.as("%s bean was created", beanName).isFalse();
		}
	}

	private void beanCreated(String beanName, String... contextProperties) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class,
				contextProperties)) {
			then(context.containsBeanDefinition(beanName))
					.as("%s bean was not created", beanName).isTrue();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void beanCreatedAndEndpointEnabled(String beanName, Class<T> type,
			Function<T, Object> function, String... properties) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class,
				properties)) {
			then(context.containsBeanDefinition(beanName))
					.as("%s bean was not created", beanName).isTrue();

			Object endpoint = context.getBean(beanName, type);
			Object result = function.apply((T) endpoint);

			then(result).as("result is wrong type").isNotInstanceOf(ResponseEntity.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class Config {

	}

}
