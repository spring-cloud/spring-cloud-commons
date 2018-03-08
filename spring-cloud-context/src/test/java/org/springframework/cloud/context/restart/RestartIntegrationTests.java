/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.cloud.context.restart;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.LiveBeansView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

public class RestartIntegrationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testRestartTwice() throws Exception {

		context = SpringApplication.run(TestConfiguration.class,
				"--management.endpoint.restart.enabled=true", "--server.port=0",
				"--spring.liveBeansView.mbeanDomain=livebeans");

		RestartEndpoint endpoint = context.getBean(RestartEndpoint.class);
		assertNotNull(context.getParent());
		assertNull(context.getParent().getParent());
		context = endpoint.doRestart();

		assertNotNull(context);
		assertNotNull(context.getParent());
		assertNull(context.getParent().getParent());

		RestartEndpoint next = context.getBean(RestartEndpoint.class);
		assertNotSame(endpoint, next);
		context = next.doRestart();

		assertNotNull(context);
		assertNotNull(context.getParent());
		assertNull(context.getParent().getParent());

		LiveBeansView beans = new LiveBeansView();
		String json = beans.getSnapshotAsJson();
		assertThat(json).containsOnlyOnce("parent\": \"bootstrap");
		assertThat(json).containsOnlyOnce("parent\": null");
	}

	public static void main(String[] args) {
		SpringApplication.run(TestConfiguration.class, args);
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class TestConfiguration {
	}

}
