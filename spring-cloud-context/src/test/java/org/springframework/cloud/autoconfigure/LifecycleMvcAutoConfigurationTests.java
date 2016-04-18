package org.springframework.cloud.autoconfigure;

import org.junit.Test;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.context.environment.EnvironmentManagerMvcEndpoint;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
public class LifecycleMvcAutoConfigurationTests {

	@Test
	public void postEnvMvcEndpointDisabled() {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class,
				"server.port=0", "endpoints.env.post.enabled=false")) {
			assertThat(context
					.getBeanNamesForType(EnvironmentManagerMvcEndpoint.class).length,
					is(equalTo(0)));
		}
	}

	@Test
	public void pauseMvcEndpointDisabled() {
		endpointDisabled("endpoints.pause.enabled", "pauseMvcEndpoint");
	}

	@Test
	public void resumeMvcEndpointDisabled() {
		endpointDisabled("endpoints.resume.enabled", "resumeMvcEndpoint");
	}

	@Test
	public void restartMvcEndpointDisabled() {
		endpointDisabled("endpoints.restart.enabled", "restartMvcEndpoint");
	}

	private void endpointDisabled(String enabledProp, String beanName) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class,
				"server.port=0", enabledProp + "=false")) {
			EndpointMvcAdapter endpoint = context.getBean(beanName,
					EndpointMvcAdapter.class);
			Object result = endpoint.invoke();
			assertThat("result is wrong type", result,
					is(instanceOf(ResponseEntity.class)));
			ResponseEntity<?> response = (ResponseEntity<?>) result;
			assertThat("response code was wrong", response.getStatusCode(),
					equalTo(HttpStatus.NOT_FOUND));
		}
	}

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration, String... properties) {
		return new SpringApplicationBuilder(configuration).properties(properties).run();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}
}
