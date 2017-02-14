package org.springframework.cloud.bootstrap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.bootstrap.BootstrapOrderingSpringApplicationJsonIntegrationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration.firstToBeCreated;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class BootstrapSourcesOrderingTests {

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	public void sourcesAreOrderedCorrectly() {
		Class<?> firstConstructedClass = firstToBeCreated.get();
		Assert.assertEquals("bootstrap sources not ordered correctly",
				TestHigherPriorityBootstrapConfiguration.class, firstConstructedClass);
	}

	@EnableAutoConfiguration
	@Configuration
	protected static class Application {
	}

}
