/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.context.refresh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author arimu1
 */
class ContextRefresherConfigTreeTests {

	private static final String CONFIG_TREE_SOURCE = "configtree";

	@TempDir
	Path configDir;

	@Test
	void configTreePropertyValuesWithoutEqualsAreComparedByContent() throws IOException {
		Files.writeString(configDir.resolve("app.message"), "hello");

		ConfigTreePropertySource first = new ConfigTreePropertySource(CONFIG_TREE_SOURCE, configDir);
		ConfigTreePropertySource second = new ConfigTreePropertySource(CONFIG_TREE_SOURCE, configDir);

		Object firstValue = first.getProperty("app.message");
		Object secondValue = second.getProperty("app.message");
		assertThat(firstValue).isNotNull();
		assertThat(Objects.equals(firstValue, secondValue)).isFalse();
		assertThat(firstValue.toString()).isEqualTo(secondValue.toString());
	}

	@Test
	void refreshEnvironmentDoesNotReportUnchangedConfigTreeProperties() throws IOException {
		Files.writeString(configDir.resolve("app.message"), "hello");

		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new ConfigTreePropertySource(CONFIG_TREE_SOURCE, configDir));

		ConfigurableApplicationContext context = Mockito.mock(ConfigurableApplicationContext.class);
		when(context.getEnvironment()).thenReturn(environment);

		RefreshScope scope = Mockito.mock(RefreshScope.class);
		ContextRefresher refresher = new ConfigDataContextRefresher(context, scope,
				new RefreshAutoConfiguration.RefreshProperties()) {
			@Override
			protected void updateEnvironment() {
				environment.getPropertySources()
					.replace(CONFIG_TREE_SOURCE, new ConfigTreePropertySource(CONFIG_TREE_SOURCE, configDir));
			}
		};

		Set<String> changedKeys = refresher.refreshEnvironment();

		assertThat(changedKeys).isEmpty();
	}

	@Test
	void refreshEnvironmentReportsChangedConfigTreePropertyContent() throws IOException {
		Path beforeDir = Files.createTempDirectory(configDir, "before");
		Path afterDir = Files.createTempDirectory(configDir, "after");
		Files.writeString(beforeDir.resolve("app.message"), "hello");
		Files.writeString(afterDir.resolve("app.message"), "goodbye");

		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new ConfigTreePropertySource(CONFIG_TREE_SOURCE, beforeDir));

		ConfigurableApplicationContext context = Mockito.mock(ConfigurableApplicationContext.class);
		when(context.getEnvironment()).thenReturn(environment);

		RefreshScope scope = Mockito.mock(RefreshScope.class);
		ContextRefresher refresher = new ConfigDataContextRefresher(context, scope,
				new RefreshAutoConfiguration.RefreshProperties()) {
			@Override
			protected void updateEnvironment() {
				environment.getPropertySources()
					.replace(CONFIG_TREE_SOURCE, new ConfigTreePropertySource(CONFIG_TREE_SOURCE, afterDir));
			}
		};

		Set<String> changedKeys = refresher.refreshEnvironment();

		assertThat(changedKeys).containsExactly("app.message");
	}

}
