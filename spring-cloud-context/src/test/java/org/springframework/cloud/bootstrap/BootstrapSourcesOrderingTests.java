package org.springframework.cloud.bootstrap;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.BootstrapOrderingSpringApplicationJsonIntegrationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration.firstToBeCreated;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class BootstrapSourcesOrderingTests {

	@Test
	public void sourcesAreOrderedCorrectly() {
		Class<?> firstConstructedClass = firstToBeCreated.get();
		assertThat(firstConstructedClass).as("bootstrap sources not ordered correctly")
				.isEqualTo(TestHigherPriorityBootstrapConfiguration.class);
	}

	@EnableAutoConfiguration
	@Configuration
	protected static class Application {
	}

}
