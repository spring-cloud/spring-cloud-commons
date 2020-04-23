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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpointTests.TestServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = ServiceRegistryEndpointNoRegistrationTests.TestConfiguration.class)
@AutoConfigureMockMvc
public class ServiceRegistryEndpointNoRegistrationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	public void testGet() throws Exception {
		this.mvc.perform(get("/service-registry/instance-status"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testPost() throws Exception {
		this.mvc.perform(post("/service-registry/instance-status").content("newstatus"))
				.andExpect(status().isNotFound());
	}

	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, EndpointAutoConfiguration.class,
			WebMvcAutoConfiguration.class
			// ManagementServerPropertiesAutoConfiguration.class
	})
	@Configuration(proxyBeanMethods = false)
	public static class TestConfiguration {

		@Bean
		ServiceRegistryEndpoint serviceRegistryEndpoint() {
			return new ServiceRegistryEndpoint(serviceRegistry());
		}

		@Bean
		ServiceRegistry serviceRegistry() {
			return new TestServiceRegistry();
		}

	}

}
