package org.springframework.cloud.client.serviceregistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.ClassPathExclusions;
import org.springframework.cloud.FilteredClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Spencer Gibb
 */
@RunWith(FilteredClassPathRunner.class)
@ClassPathExclusions({"spring-boot-actuator-*.jar", "spring-boot-starter-actuator-*.jar"})
public class ServiceRegistryAutoConfigurationTests {

	@Test
	public void runsWithoutActuator() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfig.class).web(false).run();
		try {
			context.getBean("serviceRegistryEndpoint");
			fail("found a bean that shouldn't be there");
		} catch (NoSuchBeanDefinitionException e) {
			// success
		}
	}

	@Configuration
	protected static class TestConfig {

	}
}
