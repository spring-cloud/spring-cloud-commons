package org.springframework.cloud.client.discovery.simple.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
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
	public void shouldCreateSimpleReactiveDiscoveryClient() {
		this.contextRunner.run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client.getOrder()).isEqualTo(ReactiveDiscoveryClient.DEFAULT_ORDER);
			InetUtils inet = context.getBean(InetUtils.class);
			assertThat(inet).isNotNull();
			SimpleDiscoveryProperties properties = context.getBean(SimpleDiscoveryProperties.class);
			assertThat(properties).isNotNull();
			assertThat(properties.getLocal().getServiceId()).isEqualTo("application");
			assertThat(properties.getLocal().getHost()).isEqualTo(inet.findFirstNonLoopbackHostInfo().getHostname());
			assertThat(properties.getLocal().getPort()).isEqualTo(8080);
		});

		this.contextRunner.withUserConfiguration(Configuration.class).run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client.getOrder()).isEqualTo(ReactiveDiscoveryClient.DEFAULT_ORDER);
			InetUtils inet = context.getBean(InetUtils.class);
			assertThat(inet).isNotNull();
			SimpleDiscoveryProperties properties = context.getBean(SimpleDiscoveryProperties.class);
			assertThat(properties).isNotNull();
			assertThat(properties.getLocal().getServiceId()).isEqualTo("application");
			assertThat(properties.getLocal().getHost()).isEqualTo(inet.findFirstNonLoopbackHostInfo().getHostname());
			assertThat(properties.getLocal().getPort()).isEqualTo(8080);
		});

		this.contextRunner.withUserConfiguration(Configuration.class)
				.withPropertyValues("spring.application.name=my-service",
						"spring.cloud.discovery.client.simple.order=1",
						"server.port=8443")
				.run((context) -> {
			ReactiveDiscoveryClient client = context.getBean(ReactiveDiscoveryClient.class);
			assertThat(client).isNotNull();
			assertThat(client.getOrder()).isEqualTo(1);
			InetUtils inet = context.getBean(InetUtils.class);
			assertThat(inet).isNotNull();
			SimpleDiscoveryProperties properties = context.getBean(SimpleDiscoveryProperties.class);
			assertThat(properties).isNotNull();
					assertThat(properties.getLocal().getServiceId()).isEqualTo("my-service");
			assertThat(properties.getLocal().getHost()).isEqualTo(inet.findFirstNonLoopbackHostInfo().getHostname());
			assertThat(properties.getLocal().getPort()).isEqualTo(8443);
		});
	}

	@TestConfiguration
	@EnableConfigurationProperties(ServerProperties.class)
	static class Configuration {

	}

}