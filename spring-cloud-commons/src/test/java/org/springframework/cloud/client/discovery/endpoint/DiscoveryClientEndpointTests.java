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

package org.springframework.cloud.client.discovery.endpoint;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties.SimpleServiceInstance;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "management.endpoints.web.exposure.include=discovery" },
		webEnvironment = RANDOM_PORT)
@ActiveProfiles("lbendpoint")
public class DiscoveryClientEndpointTests {

	@Autowired
	private WebTestClient client;

	@Test
	public void servicesWorks() {
		client.get().uri("/actuator/discovery").exchange().expectBody(List.class)
				.consumeWith(result -> {
					List<String> body = result.getResponseBody();
					assertThat(body).contains("myservice", "anotherservice",
							"thirdservice");
				});

	}

	@Test
	public void instancesWorks() {
		client.get().uri("/actuator/discovery/myservice").exchange()
				.expectBodyList(SimpleServiceInstance.class).consumeWith(result -> {
					List body = result.getResponseBody();
					assertThat(body).isNotEmpty();
				});

	}

	@Test
	public void noInstancesWorks() {
		client.get().uri("/actuator/discovery/unknownservice").exchange()
				.expectBodyList(SimpleServiceInstance.class).consumeWith(result -> {
					List body = result.getResponseBody();
					assertThat(body).isEmpty();
				});

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestConfiguration {

	}

}
