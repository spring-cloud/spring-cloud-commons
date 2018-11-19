package org.springframework.cloud.client.discovery;

import org.junit.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedRuntimeException;

import static org.junit.Assert.assertTrue;

/**
 * Tests that if <code>@EnableDiscoveryClient</code> is used, but there is no
 * implementation on the classpath, then fail
 * @author Spencer Gibb
 */
public class EnableDiscoveryClientMissingImplTests {

	@Test
	public void testContextFails() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(App.class).web(WebApplicationType.NONE).run(new String[0]);) {
		}
		catch (NestedRuntimeException e) {
			Throwable rootCause = e.getRootCause();
			assertTrue(rootCause instanceof IllegalStateException);
			assertTrue(rootCause.getMessage().contains("no implementations"));
		}
	}

	@EnableAutoConfiguration
	@Configuration
	@EnableDiscoveryClient
	// this will fail with @EnableDiscoveryClient and no implementation (nothing in
	// spring.factories)
	public static class App {
	}
}
