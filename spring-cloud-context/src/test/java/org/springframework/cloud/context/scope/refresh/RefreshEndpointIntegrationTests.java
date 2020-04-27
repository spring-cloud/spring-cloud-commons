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

package org.springframework.cloud.context.scope.refresh;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshEndpointIntegrationTests.ClientApp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ClientApp.class,
		properties = { "management.endpoints.web.exposure.include=*",
				"management.endpoint.env.post.enabled=true" },
		webEnvironment = RANDOM_PORT)
public class RefreshEndpointIntegrationTests {

	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	@LocalServerPort
	private int port;

	@Test
	public void webAccess() throws Exception {
		TestRestTemplate template = new TestRestTemplate();
		template.exchange(
				getUrlEncodedEntity("http://localhost:" + this.port + BASE_PATH + "/env",
						"message", "Hello Dave!"),
				String.class);
		template.postForObject("http://localhost:" + this.port + BASE_PATH + "/refresh",
				null, String.class);
		String message = template.getForObject("http://localhost:" + this.port + "/",
				String.class);
		then(message).isEqualTo("Hello Dave!");
	}

	private RequestEntity<?> getUrlEncodedEntity(String uri, String key, String value)
			throws URISyntaxException {
		Map<String, String> property = new HashMap<>();
		property.put("name", key);
		property.put("value", value);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		RequestEntity<Map<String, String>> entity = new RequestEntity<>(property, headers,
				HttpMethod.POST, new URI(uri));
		return entity;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class ClientApp {

		public static void main(String[] args) {
			SpringApplication.run(ClientApp.class, args);
		}

		@Bean
		@RefreshScope
		public Controller controller() {
			return new Controller();
		}

	}

	@RestController
	protected static class Controller {

		String message;

		@Value("${message:Hello World!}")
		public void setMessage(String message) {
			this.message = message;
		}

		@RequestMapping("/")
		public String hello() {
			return this.message;
		}

	}

}
