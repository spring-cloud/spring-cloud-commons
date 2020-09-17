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

package org.springframework.cloud.client.discovery.composite.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Ysewyn
 */
class ReactiveCompositeDiscoveryClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveCompositeDiscoveryClientAutoConfiguration.class));

	@Test
	public void shouldCreateCompositeReactiveDiscoveryClientWithoutDelegates() {
		this.contextRunner.run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client).isInstanceOf(ReactiveCompositeDiscoveryClient.class);
			assertThat(((ReactiveCompositeDiscoveryClient) client).getDiscoveryClients()).isEmpty();
		});
	}

	@Test
	public void shouldCreateCompositeReactiveDiscoveryClientWithDelegate() {
		this.contextRunner.withUserConfiguration(Configuration.class).run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client).isInstanceOf(ReactiveCompositeDiscoveryClient.class);
			assertThat(((ReactiveCompositeDiscoveryClient) client).getDiscoveryClients()).hasSize(1);
		});
	}

	@TestConfiguration
	static class Configuration {

		@Bean
		ReactiveDiscoveryClient discoveryClient() {
			return new ReactiveDiscoveryClient() {
				@Override
				public String description() {
					return "Reactive Test Discovery Client";
				}

				@Override
				public Flux<ServiceInstance> getInstances(String serviceId) {
					return Flux.empty();
				}

				@Override
				public Flux<String> getServices() {
					return Flux.empty();
				}
			};
		}

	}

}
