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

package org.springframework.cloud.context.properties;

import java.util.List;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link ConfigurationPropertiesRebinder#isResettableNestedType}.
 *
 * @author Ryan Baxter
 */
public class ConfigurationPropertiesRebinderTests {

	private final ConfigurationPropertiesRebinder rebinder = new ConfigurationPropertiesRebinder(
			new ConfigurationPropertiesBeans());

	@Test
	public void userDefinedTypeIsResettable() {
		then(this.rebinder.isResettableNestedType(NestedProperties.class)).isTrue();
	}

	@Test
	public void simpleValueAndArrayTypesAreNotResettable() {
		then(this.rebinder.isResettableNestedType(String.class)).isFalse();
		then(this.rebinder.isResettableNestedType(Integer.class)).isFalse();
		then(this.rebinder.isResettableNestedType(NestedProperties[].class)).isFalse();
	}

	@Test
	public void jdkTypesAreNotResettable() {
		// Loaded by the bootstrap (java.base) or platform (java.sql) class loader
		then(this.rebinder.isResettableNestedType(SSLContext.class)).isFalse();
		then(this.rebinder.isResettableNestedType(List.class)).isFalse();
		then(this.rebinder.isResettableNestedType(DataSource.class)).isFalse();
	}

	@Test
	public void jakartaApiTypesAreNotResettable() {
		// Loaded by the application class loader, so not detected as a JDK type by the
		// class loader check; only caught by the standard API namespace check.
		then(EntityManager.class.getClassLoader()).isNotNull();
		then(this.rebinder.isResettableNestedType(EntityManager.class)).isFalse();
		then(this.rebinder.isResettableNestedType(PostConstruct.class)).isFalse();
	}

	protected static class NestedProperties {

		private String host = "default-host";

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

	}

}
