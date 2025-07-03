/*
 * Copyright 2013-2025 the original author or authors.
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

package org.springframework.cloud.context.integration.webflux;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.test.LocalServerPort;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Dave Syer
 *
 */
@SpringBootTest(classes = WebfluxRefreshEndpointIntegrationTests.ClientApp.class,
		properties = { "management.endpoints.web.exposure.include=*" }, webEnvironment = RANDOM_PORT)
public class WebfluxRefreshEndpointIntegrationTests {

	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	@LocalServerPort
	private int port;

	@Test
	public void webAccess() throws Exception {
		TestRestTemplate template = new TestRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity request = new HttpEntity(headers);
		ResponseEntity<String> entity = template.postForEntity("http://localhost:" + this.port + BASE_PATH + "/refresh",
				request, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class ClientApp {

	}

}
