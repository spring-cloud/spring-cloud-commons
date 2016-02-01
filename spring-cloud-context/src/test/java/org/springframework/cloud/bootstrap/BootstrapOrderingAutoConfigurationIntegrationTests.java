package org.springframework.cloud.bootstrap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.bootstrap.BootstrapOrderingAutoConfigurationIntegrationTests.Application;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest("encrypt.key:deadbeef")
@ActiveProfiles("encrypt")
public class BootstrapOrderingAutoConfigurationIntegrationTests {

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
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
