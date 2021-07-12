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

package org.springframework.cloud.client.serviceregistry.endpoint;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
// @checkstyle:off
@SpringBootTest(classes = ServiceRegistryEndpointTests.TestConfiguration.class,
		properties = "management.endpoints.web.exposure.include=*")
// @checkstyle:on
@AutoConfigureMockMvc
public class ServiceRegistryEndpointTests {

	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	private static final String UPDATED_STATUS = "updatedstatus";

	private static final String MYSTATUS = "mystatus";

	@Autowired
	private TestServiceRegistry serviceRegistry;

	@Autowired
	private MockMvc mvc;

	@Test
	public void testGet() throws Exception {
		this.mvc.perform(get(BASE_PATH + "/serviceregistry")).andExpect(status().isOk())
				.andExpect(content().string(containsString(MYSTATUS)));
	}

	@Test
	public void testPost() throws Exception {
		Map<String, String> status = Collections.singletonMap("status", UPDATED_STATUS);
		this.mvc.perform(post(BASE_PATH + "/serviceregistry").content(new ObjectMapper().writeValueAsString(status))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		then(this.serviceRegistry.getUpdatedStatus().get()).isEqualTo(UPDATED_STATUS);
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	public static class TestConfiguration {

		@Bean
		Registration registration() {
			return new Registration() {
				@Override
				public String getInstanceId() {
					return "testRegistrationInstance1";
				}

				@Override
				public String getServiceId() {
					return "testRegistration1";
				}

				@Override
				public String getHost() {
					return null;
				}

				@Override
				public int getPort() {
					return 0;
				}

				@Override
				public boolean isSecure() {
					return false;
				}

				@Override
				public URI getUri() {
					return null;
				}

				@Override
				public Map<String, String> getMetadata() {
					return null;
				}
			};
		}

		@Bean
		ServiceRegistry serviceRegistry() {
			return new TestServiceRegistry();
		}

	}

	static class TestServiceRegistry implements ServiceRegistry {

		AtomicReference<String> updatedStatus = new AtomicReference<>();

		@Override
		public void register(Registration registration) {

		}

		@Override
		public void deregister(Registration registration) {

		}

		@Override
		public void close() {

		}

		@Override
		public void setStatus(Registration registration, String status) {
			this.updatedStatus.compareAndSet(null, status);
		}

		@Override
		public Object getStatus(Registration registration) {
			return MYSTATUS;
		}

		public AtomicReference<String> getUpdatedStatus() {
			return this.updatedStatus;
		}

	}

}
