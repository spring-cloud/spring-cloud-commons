package org.springframework.cloud.bootstrap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.BootstrapOrderingSpringApplicationJsonIntegrationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class,
		properties = "spring.cloud.bootstrap.name:json")
public class BootstrapOrderingSpringApplicationJsonIntegrationTests {

	@BeforeClass
	public static void spikeJson() {
		System.setProperty("SPRING_APPLICATION_JSON", "{\"message\":\"From JSON\"}");
	}

	@AfterClass
	public static void unspikeJson() {
		System.clearProperty("SPRING_APPLICATION_JSON");
	}

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	public void bootstrapPropertiesExist() {
		assertTrue(this.environment.getPropertySources()
				.contains("spring.application.json"));
		assertEquals("From JSON", this.environment.getProperty("message"));
	}

	@EnableAutoConfiguration
	@Configuration
	protected static class Application {
	}

}
