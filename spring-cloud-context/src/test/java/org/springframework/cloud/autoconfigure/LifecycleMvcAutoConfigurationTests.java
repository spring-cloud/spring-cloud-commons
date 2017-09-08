package org.springframework.cloud.autoconfigure;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
//TODO: super slow. Port to @SpringBootTest
public class LifecycleMvcAutoConfigurationTests {

	@Test
	public void environmentWebEndpointExtensionDisabled() {
		beanNotCreated("environmentWebEndpointExtension",
				"endpoints.env.enabled=false");
	}

	@Test
	public void environmentWebEndpointExtensionGloballyDisabled() {
		beanNotCreated("environmentWebEndpointExtension",
				"endpoints.default.enabled=false");
	}

	@Test
	public void environmentWebEndpointExtensionEnabled() {
		beanCreated("environmentWebEndpointExtension",
				"endpoints.env.enabled=true");
	}

	// restartEndpoint
	@Test
	public void restartEndpointDisabled() {
		beanNotCreated("restartEndpoint",
				"endpoints.restart.enabled=false");
	}

	@Test
	public void restartEndpointGloballyDisabled() {
		beanNotCreated("restartEndpoint",
				"endpoints.default.enabled=false");
	}

	@Test
	public void restartEndpointEnabled() {
		beanCreatedAndEndpointEnabled("restartEndpoint", RestartEndpoint.class,
				RestartEndpoint::restart,
				"endpoints.restart.enabled=true");
	}

	// pauseEndpoint
	@Test
	public void pauseEndpointDisabled() {
		beanNotCreated("pauseEndpoint",
				"endpoints.pause.enabled=false");
	}

	@Test
	public void pauseEndpointRestartDisabled() {
		beanNotCreated("pauseEndpoint",
				"endpoints.restart.enabled=false",
				"endpoints.pause.enabled=true");
	}

	@Test
	public void pauseEndpointGloballyDisabled() {
		beanNotCreated("pauseEndpoint",
				"endpoints.default.enabled=false");
	}

	@Test
	public void pauseEndpointEnabled() {
		beanCreatedAndEndpointEnabled("pauseEndpoint", RestartEndpoint.PauseEndpoint.class,
				RestartEndpoint.PauseEndpoint::pause,
				"endpoints.restart.enabled=true",
				"endpoints.pause.enabled=true");
	}

	// resumeEndpoint
	@Test
	public void resumeEndpointDisabled() {
		beanNotCreated("resumeEndpoint",
				"endpoints.restart.enabled=true",
				"endpoints.resume.enabled=false");
	}

	@Test
	public void resumeEndpointRestartDisabled() {
		beanNotCreated("resumeEndpoint",
				"endpoints.restart.enabled=false",
				"endpoints.resume.enabled=true");
	}

	@Test
	public void resumeEndpointGloballyDisabled() {
		beanNotCreated("resumeEndpoint",
				"endpoints.default.enabled=false");
	}

	@Test
	public void resumeEndpointEnabled() {
		beanCreatedAndEndpointEnabled("resumeEndpoint", RestartEndpoint.ResumeEndpoint.class,
				RestartEndpoint.ResumeEndpoint::resume,
				"endpoints.restart.enabled=true",
				"endpoints.resume.enabled=true");
	}

	private void beanNotCreated(String beanName, String... contextProperties) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class, contextProperties)) {
			assertThat("bean was created", context.containsBeanDefinition(beanName), equalTo(false));
		}
	}

	private void beanCreated(String beanName, String... contextProperties) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class, contextProperties)) {
			assertThat("bean was not created", context.containsBeanDefinition(beanName), equalTo(true));
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void beanCreatedAndEndpointEnabled(String beanName, Class<T> type, Function<T, Object> function, String... properties) {
		try (ConfigurableApplicationContext context = getApplicationContext(Config.class, properties)) {
			assertThat("bean was not created", context.containsBeanDefinition(beanName), equalTo(true));

			Object endpoint = context.getBean(beanName, type);
			Object result = function.apply((T) endpoint);

			assertThat("result is wrong type", result,
					is(not(instanceOf(ResponseEntity.class))));
		}
	}

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration, String... properties) {

		List<String> defaultProperties = Lists.newArrayList(properties);
		defaultProperties.add("server.port=0");
		defaultProperties.add("spring.jmx.default-domain=${random.uuid}");

		return new SpringApplicationBuilder(configuration).properties(defaultProperties.toArray(new String[]{})).run();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}
}
