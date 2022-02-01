/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
@ExtendWith(OutputCaptureExtension.class)
public class ConfigDataMissingEnvironmentPostProcessorTests {

	@Test
	void noSpringConfigImport() {
		MockEnvironment environment = new MockEnvironment();
		SpringApplication app = mock(SpringApplication.class);
		TestConfigDataMissingEnvironmentPostProcessor processor = new TestConfigDataMissingEnvironmentPostProcessor();
		assertThatThrownBy(() -> processor.postProcessEnvironment(environment, app))
				.isInstanceOf(ConfigDataMissingEnvironmentPostProcessor.ImportException.class);
	}

	@Test
	void importSinglePropertySource() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.config.import", "configserver:http://localhost:8888");
		SpringApplication app = mock(SpringApplication.class);
		TestConfigDataMissingEnvironmentPostProcessor processor = new TestConfigDataMissingEnvironmentPostProcessor();
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	void importMultiplePropertySource() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.config.import", "configserver:http://localhost:8888,file:./app.properties");
		SpringApplication app = mock(SpringApplication.class);
		TestConfigDataMissingEnvironmentPostProcessor processor = new TestConfigDataMissingEnvironmentPostProcessor();
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	void importMultiplePropertySourceAsList() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.config.import[0]", "configserver:http://localhost:8888");
		environment.setProperty("spring.config.import[1]", "file:./app.properties");
		SpringApplication app = mock(SpringApplication.class);
		TestConfigDataMissingEnvironmentPostProcessor processor = new TestConfigDataMissingEnvironmentPostProcessor();
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	void importCompositePropertySource() {
		MockEnvironment environment = new MockEnvironment();
		CompositePropertySource ps1 = new CompositePropertySource("ps1");
		MockPropertySource ps2 = new MockPropertySource("ps2");
		ps2.setProperty("spring.config.import", "file:./app.properties");
		MockPropertySource ps3 = new MockPropertySource("ps3");
		ps3.setProperty("my.property", "value");
		MockPropertySource ps4 = new MockPropertySource("ps4");
		ps4.setProperty("spring.config.import[0]", "file:./moreproperties.yaml");
		ps4.setProperty("spring.config.import[1]", "configserver:http://localhost:8888");
		CompositePropertySource compositePropertySource = new CompositePropertySource("composite");
		compositePropertySource.addPropertySource(ps3);
		compositePropertySource.addPropertySource(ps4);
		ps1.addPropertySource(compositePropertySource);
		environment.getPropertySources().addFirst(ps2);
		environment.getPropertySources().addLast(ps1);
		SpringApplication app = mock(SpringApplication.class);
		TestConfigDataMissingEnvironmentPostProcessor processor = new TestConfigDataMissingEnvironmentPostProcessor();
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	void importHandlesNullConfigurationPropertySource(CapturedOutput output) {
		MockEnvironment environment = new MockEnvironment();
		ConfigurationPropertySources.attach(environment);
		environment.setProperty("spring.config.import[0]", "configserver:http://localhost:8888");
		environment.setProperty("spring.config.import[1]", "file:./app.properties");
		SpringApplication app = mock(SpringApplication.class);
		TestConfigDataMissingEnvironmentPostProcessor processor = new TestConfigDataMissingEnvironmentPostProcessor();
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
		assertThat(output).doesNotContain("Error binding spring.config.import");
	}

	public class TestConfigDataMissingEnvironmentPostProcessor extends ConfigDataMissingEnvironmentPostProcessor {

		@Override
		protected boolean shouldProcessEnvironment(Environment environment) {
			return true;
		}

		@Override
		protected String getPrefix() {
			return "configserver:";
		}

	}

}
