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

package org.springframework.cloud.client.discovery;

import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedRuntimeException;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests that if <code>@EnableDiscoveryClient</code> is used, but there is no
 * implementation on the classpath, then fail
 *
 * @author Spencer Gibb
 */
public class EnableDiscoveryClientMissingImplTests {

	@Test
	public void testContextFails() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(App.class).web(WebApplicationType.NONE).run()) {
			// do sth
		}
		catch (NestedRuntimeException e) {
			Throwable rootCause = e.getRootCause();
			then(rootCause instanceof IllegalStateException).isTrue();
			then(rootCause.getMessage().contains("no implementations")).isTrue();
		}
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@EnableDiscoveryClient
	// this will fail with @EnableDiscoveryClient and no implementation (nothing in
	// spring.factories)
	public static class App {

	}

}
