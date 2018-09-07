package org.springframework.cloud.client;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.actuator.FeaturesEndpoint;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthIndicator;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Spencer Gibb
 */
public class CommonsClientAutoConfigurationTests {

	@Test
	public void beansCreatedNormally() {
		try (ConfigurableApplicationContext ctxt = init()) {
			assertThat(ctxt.getBean(DiscoveryClientHealthIndicator.class),
					is(notNullValue()));
			assertThat(ctxt.getBean(DiscoveryCompositeHealthIndicator.class),
					is(notNullValue()));
			assertThat(ctxt.getBean(FeaturesEndpoint.class), is(notNullValue()));
			assertThat(ctxt.getBeansOfType(HasFeatures.class).values(),
					not(emptyCollectionOf(HasFeatures.class)));
		}
	}

	@Test
	public void disableAll() {
		try (ConfigurableApplicationContext ctxt = init("spring.cloud.discovery.enabled=false")) {
			assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
			assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
			assertThat(ctxt.getBean(FeaturesEndpoint.class), is(notNullValue())); // features actuator is independent of discovery
			assertBeanNonExistant(ctxt, HasFeatures.class);
		}
	}

	@Test
	public void disableAllIndividually() {
		try (ConfigurableApplicationContext ctxt = init(
				"spring.cloud.discovery.client.health-indicator.enabled=false",
				"spring.cloud.discovery.client.composite-indicator.enabled=false",
				"spring.cloud.features.enabled=false")) {
			assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
			assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
			assertBeanNonExistant(ctxt, FeaturesEndpoint.class);
		}
	}

	@Test
	public void disableHealthIndicator() {
		try (ConfigurableApplicationContext ctxt = init(
				"spring.cloud.discovery.client.health-indicator.enabled=false")) {
			assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
			assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
		}
	}

	private void assertBeanNonExistant(ConfigurableApplicationContext ctxt,
			Class<?> beanClass) {
		try {
			ctxt.getBean(beanClass);
			fail("Bean of type " + beanClass + " should not have been created");
		}
		catch (BeansException e) {
			// should fail with exception
		}
	}

	protected ConfigurableApplicationContext init(String... pairs) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(Config.class)
				.properties(pairs).run();
	}

	@Configuration
	@EnableAutoConfiguration
	@Import(NoopDiscoveryClientAutoConfiguration.class)
	protected static class Config {
	}
}
