/*
 * Copyright 2012-present the original author or authors.
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

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.DefaultServiceInstance;
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

	@ParameterizedTest
	@ValueSource(strings = { "", "spring.cloud.discovery.reactive.order-enforced=false" })
	void shouldUseFastestClientWhenOrderIsNotEnforced(String property) {
		ApplicationContextRunner runner = this.contextRunner.withUserConfiguration(OrderedConfiguration.class);
		if (!property.isEmpty()) {
			runner = runner.withPropertyValues(property);
		}
		runner.run(context -> {
			assertThat(context).hasSingleBean(ReactiveCompositeDiscoveryClient.class);
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			StepVerifier.withVirtualTime(() -> client.getInstances("service"))
				.assertNext(instance -> assertThat(instance.getInstanceId()).isEqualTo("second"))
				.verifyComplete();
		});
	}

	@Test
	void shouldEnforceOrderWhenEnabled() {
		this.contextRunner.withUserConfiguration(OrderedConfiguration.class)
			.withPropertyValues("spring.cloud.discovery.reactive.order-enforced=true")
			.run(context -> {
				assertThat(context).hasSingleBean(ReactiveCompositeDiscoveryClient.class);
				ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
				StepVerifier.withVirtualTime(() -> client.getInstances("service"))
					.expectSubscription()
					.expectNoEvent(Duration.ofSeconds(1))
					.thenAwait(Duration.ofSeconds(1))
					.assertNext(instance -> assertThat(instance.getInstanceId()).isEqualTo("preferred"))
					.verifyComplete();
			});
	}

	@TestConfiguration
	static class OrderedConfiguration {

		@Bean
		ReactiveDiscoveryClient preferredClient() {
			return discoveryClient(-1, "preferred");
		}

		@Bean
		ReactiveDiscoveryClient secondClient() {
			return discoveryClient(0, "second");
		}

		private ReactiveDiscoveryClient discoveryClient(int order, String instanceId) {
			return new ReactiveDiscoveryClient() {

				@Override
				public String description() {
					return instanceId;
				}

				@Override
				public int getOrder() {
					return order;
				}

				@Override
				public Flux<ServiceInstance> getInstances(String serviceId) {
					Flux<ServiceInstance> instances = Flux
						.just(new DefaultServiceInstance(instanceId, serviceId, "localhost", 8080, false));
					return order < 0 ? instances.delayElements(Duration.ofSeconds(2)) : instances;
				}

				@Override
				public Flux<String> getServices() {
					return Flux.empty();
				}
			};
		}

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
