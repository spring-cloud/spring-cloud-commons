package org.springframework.cloud.bootstrap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.BootstrapOrderingAutoConfigurationIntegrationTests.Application;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, properties = "encrypt.key:deadbeef")
@ActiveProfiles("encrypt")
public class BootstrapOrderingAutoConfigurationIntegrationTests {

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@Ignore //FIXME: spring boot 2.0.0
	public void bootstrapPropertiesExist() {
		assertTrue(this.environment.getPropertySources().contains(
				PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME));
	}

	@Test
	public void normalPropertiesDecrypted() {
		assertEquals("foo", this.environment.resolvePlaceholders("${foo}"));
	}

	@Test
	public void bootstrapPropertiesDecrypted() {
		assertEquals("bar", this.environment.resolvePlaceholders("${bar}"));
	}

	@EnableAutoConfiguration
	@Configuration
	protected static class Application {

	}

}
