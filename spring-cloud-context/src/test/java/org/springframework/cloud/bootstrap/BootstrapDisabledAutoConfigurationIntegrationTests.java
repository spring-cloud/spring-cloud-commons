package org.springframework.cloud.bootstrap;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.bootstrap.BootstrapDisabledAutoConfigurationIntegrationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest("spring.cloud.bootstrap.enabled:false")
public class BootstrapDisabledAutoConfigurationIntegrationTests {
	
	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	public void noBootstrapProperties() {
		assertFalse(environment.getPropertySources().contains("bootstrap"));
	}

	@EnableAutoConfiguration
	@Configuration
	protected static class Application {

	}

}
