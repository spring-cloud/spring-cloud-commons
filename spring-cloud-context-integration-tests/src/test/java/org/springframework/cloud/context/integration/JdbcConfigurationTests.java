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

package org.springframework.cloud.context.integration;

import org.junit.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @author Dave Syer
 *
 */
public class JdbcConfigurationTests {

	@Test
	public void schemaApplied() {
		new SpringApplicationBuilder(BrokenApplication.class).web(WebApplicationType.NONE)
				.run("--spring.datasource.initialization-mode=always").close();
	}

	@SpringBootConfiguration
	@EnableConfigurationProperties(DataSourceProperties.class)
	@Import({ DataSourceAutoConfiguration.class, RefreshAutoConfiguration.class })
	protected static class BrokenApplication {

		public static void main(String[] args) {
			new SpringApplicationBuilder(BrokenApplication.class)
					.web(WebApplicationType.NONE).run(args);
		}

	}

}
