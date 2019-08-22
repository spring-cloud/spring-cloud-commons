package org.springframework.cloud.client.discovery.composite.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
			.withConfiguration(AutoConfigurations.of(
					ReactiveCompositeDiscoveryClientAutoConfiguration.class));

	@Test
	public void shouldCreateSimpleReactiveDiscoveryClient() {
		this.contextRunner.run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client).isInstanceOf(ReactiveCompositeDiscoveryClient.class);
			assertThat(((ReactiveCompositeDiscoveryClient)client).getDiscoveryClients()).isEmpty();
		});

		this.contextRunner.withUserConfiguration(Configuration.class).run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client).isInstanceOf(ReactiveCompositeDiscoveryClient.class);
			assertThat(((ReactiveCompositeDiscoveryClient)client).getDiscoveryClients()).hasSize(1);
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