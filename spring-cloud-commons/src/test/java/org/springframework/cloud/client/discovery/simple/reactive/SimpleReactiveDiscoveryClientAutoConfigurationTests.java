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

package org.springframework.cloud.client.discovery.simple.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Ysewyn
 */
class SimpleReactiveDiscoveryClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					SimpleReactiveDiscoveryClientAutoConfiguration.class,
					UtilAutoConfiguration.class));

	@Test
	public void shouldUseDefaults() {
		this.contextRunner.run((context) -> {
			ReactiveDiscoveryClient client = context
					.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client.getOrder())
					.isEqualTo(ReactiveDiscoveryClient.DEFAULT_ORDER);
			InetUtils inet = context.getBean(InetUtils.class);
			assertThat(inet).isNotNull();
			SimpleReactiveDiscoveryProperties properties = context
					.getBean(SimpleReactiveDiscoveryProperties.class);
			assertThat(properties).isNotNull();
			assertThat(properties.getLocal().getServiceId()).isEqualTo("application");
			assertThat(properties.getLocal().getHost())
					.isEqualTo(inet.findFirstNonLoopbackHostInfo().getHostname());
			assertThat(properties.getLocal().getPort()).isEqualTo(8080);
		});
	}

	@Test
	public void shouldUseCustomConfiguration() {
		this.contextRunner.withUserConfiguration(Configuration.class)
				.withPropertyValues("spring.application.name=my-service",
						"spring.cloud.discovery.client.simple.order=1",
						"server.port=8443")
				.run((context) -> {
					ReactiveDiscoveryClient client = context
							.getBean(ReactiveDiscoveryClient.class);
					assertThat(client).isNotNull();
					assertThat(client.getOrder()).isEqualTo(1);
					InetUtils inet = context.getBean(InetUtils.class);
					assertThat(inet).isNotNull();
					SimpleReactiveDiscoveryProperties properties = context
							.getBean(SimpleReactiveDiscoveryProperties.class);
					assertThat(properties).isNotNull();
					assertThat(properties.getLocal().getServiceId())
							.isEqualTo("my-service");
					assertThat(properties.getLocal().getHost())
							.isEqualTo(inet.findFirstNonLoopbackHostInfo().getHostname());
					assertThat(properties.getLocal().getPort()).isEqualTo(8443);
				});
	}

	@TestConfiguration
	@EnableConfigurationProperties(ServerProperties.class)
	static class Configuration {

	}

}
