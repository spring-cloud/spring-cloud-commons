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

package org.springframework.cloud.bootstrap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.BootstrapOrderingCustomPropertySourceIntegrationTests.Application;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class,
		properties = { "encrypt.key:deadbeef", "spring.cloud.bootstrap.name:custom" })
@ActiveProfiles("encrypt")
public class BootstrapOrderingCustomPropertySourceIntegrationTests {

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@Ignore // FIXME: spring boot 2.0.0
	public void bootstrapPropertiesExist() {
		then(this.environment.getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME))
						.isTrue();
	}

	@Test
	public void customPropertiesDecrypted() {
		then(this.environment.resolvePlaceholders("${custom.foo}")).isEqualTo("bar");
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	protected static class Application {

	}

	@Configuration(proxyBeanMethods = false)
	// This is added to bootstrap context as a source in bootstrap.properties
	protected static class PropertySourceConfiguration implements PropertySourceLocator {

		public static Map<String, Object> MAP = new HashMap<String, Object>(
				Collections.<String, Object>singletonMap("custom.foo",
						"{cipher}6154ca04d4bb6144d672c4e3d750b5147116dd381946d51fa44f8bc25dc256f4"));

		@Override
		public PropertySource<?> locate(Environment environment) {
			return new MapPropertySource("testBootstrap", MAP);
		}

	}

}
