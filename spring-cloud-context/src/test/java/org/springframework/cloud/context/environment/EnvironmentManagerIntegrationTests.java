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

package org.springframework.cloud.context.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpointWebExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentManagerIntegrationTests.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class,
		properties = { "management.endpoints.web.exposure.include=*",
				"management.endpoint.env.post.enabled=true" })
@AutoConfigureMockMvc
public class EnvironmentManagerIntegrationTests {

	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	@Autowired
	private TestProperties properties;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testRefresh() throws Exception {
		then(this.properties.getMessage()).isEqualTo("Hello scope!");
		String content = property("message", "Foo");

		this.mvc.perform(post(BASE_PATH + "/env").content(content)
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().string("{\"message\":\"Foo\"}"));
		then(this.properties.getMessage()).isEqualTo("Foo");
	}

	private String property(String name, String value) throws JsonProcessingException {
		// Change the dynamic property source...
		Map<String, String> property = new HashMap<>();
		property.put("name", name);
		property.put("value", value);

		return this.mapper.writeValueAsString(property);
	}

	@Test
	public void testRefreshFails() throws Exception {
		try {
			this.mvc.perform(post(BASE_PATH + "/env").content(property("delay", "foo"))
					.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
					.andExpect(status().is5xxServerError());
			fail("expected ServletException");
		}
		catch (ServletException e) {
			// The underlying BindException is not handled by the dispatcher servlet
		}
		then(this.properties.getDelay()).isEqualTo(0);
	}

	@Test
	public void coreWebExtensionAvailable() throws Exception {
		this.mvc.perform(get(BASE_PATH + "/env/" + UUID.randomUUID().toString()))
				.andExpect(status().isNotFound());
	}

	@Test
	public void environmentBeansConfiguredCorrectly() {
		Map<String, EnvironmentEndpoint> envbeans = this.context
				.getBeansOfType(EnvironmentEndpoint.class);
		then(envbeans).hasSize(1).containsKey("writableEnvironmentEndpoint");
		then(envbeans.get("writableEnvironmentEndpoint"))
				.isInstanceOf(WritableEnvironmentEndpoint.class);

		Map<String, EnvironmentEndpointWebExtension> extbeans = this.context
				.getBeansOfType(EnvironmentEndpointWebExtension.class);
		then(extbeans).hasSize(1).containsKey("writableEnvironmentEndpointWebExtension");
		then(extbeans.get("writableEnvironmentEndpointWebExtension"))
				.isInstanceOf(WritableEnvironmentEndpointWebExtension.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class TestConfiguration {

		@Bean
		protected TestProperties properties() {
			return new TestProperties();
		}

	}

	@ConfigurationProperties
	protected static class TestProperties {

		private String message;

		private int delay;

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getDelay() {
			return this.delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

	}

}
