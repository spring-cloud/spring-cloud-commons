/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.bootstrap;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

// see https://github.com/spring-cloud/spring-cloud-commons/issues/476
@RunWith(SpringRunner.class)
@SpringBootTest
public class BootstrapEnvironmentPostProcessorIntegrationTests {

	@Autowired
	private ConfigurableEnvironment env;

	@Test
	public void conditionalValuesFromMapProperySourceCreatedByEPPExist() {
		assertThat(this.env.containsProperty("unconditional.property"))
				.as("Environment does not contain unconditional.property").isTrue();
		assertThat(this.env.getProperty("unconditional.property"))
				.as("Environment has wrong value for unconditional.property")
				.isEqualTo("unconditional.value");
		assertThat(this.env.containsProperty("conditional.property"))
				.as("Environment does not contain conditional.property").isTrue();
		assertThat(this.env.getProperty("conditional.property"))
				.as("Environment has wrong value for conditional.property")
				.isEqualTo("conditional.value");
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	protected static class TestConfig {

	}

	public static class TestConditionalEnvironmentPostProcessor
			implements EnvironmentPostProcessor {

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment,
				SpringApplication application) {
			HashMap<String, Object> map = new HashMap<>();

			if (!environment.containsProperty("conditional.property")) {
				map.put("conditional.property", "conditional.value");
			}
			map.put("unconditional.property", "unconditional.value");

			MapPropertySource propertySource = new MapPropertySource("test-epp-map", map);
			environment.getPropertySources().addFirst(propertySource);
		}

	}

}
