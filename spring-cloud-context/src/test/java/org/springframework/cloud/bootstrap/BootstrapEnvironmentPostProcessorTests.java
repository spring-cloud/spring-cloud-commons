/*
 * Copyright 2013-2019 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BootstrapEnvironmentPostProcessorTests {

	@Autowired
	private ConfigurableEnvironment env;

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void contextLoads() {
		// from bootstrap.properties
		assertThat(env.getProperty("test.property"))
				.isEqualTo("from bootstrap.properties");

		try {
			assertThat(context.getBean("foo-during-bootstrap"));
			fail("bean created in bootstrap found");
		}
		catch (NoSuchBeanDefinitionException e) {
			// shouldn't exist
		}
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	protected static class TestConfig {

	}

}
