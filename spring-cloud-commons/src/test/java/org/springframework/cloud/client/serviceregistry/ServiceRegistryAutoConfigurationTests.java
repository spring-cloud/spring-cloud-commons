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

package org.springframework.cloud.client.serviceregistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Spencer Gibb
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "spring-boot-actuator-*.jar",
		"spring-boot-starter-actuator-*.jar" })
public class ServiceRegistryAutoConfigurationTests {

	@Test
	public void runsWithoutActuator() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestConfig.class).web(WebApplicationType.NONE).run();
		try {
			context.getBean("serviceRegistryEndpoint");
			fail("found a bean that shouldn't be there");
		}
		catch (NoSuchBeanDefinitionException e) {
			// success
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfig {

	}

}
