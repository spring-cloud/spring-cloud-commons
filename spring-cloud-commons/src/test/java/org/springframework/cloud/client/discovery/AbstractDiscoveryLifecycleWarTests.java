package org.springframework.cloud.client.discovery;

import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.client.discovery.AbstractDiscoveryLifecycleTests.TestDiscoveryLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Spencer Gibb
 */
public class AbstractDiscoveryLifecycleWarTests {

	@Test
	public void portsWork() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class);
		EnvironmentTestUtils.addEnvironment(context.getEnvironment(), "server.port=7777");
		context.refresh();

		TestDiscoveryLifecycle lifecycle = context.getBean(TestDiscoveryLifecycle.class);
		assertEquals("Lifecycle port is wrong", 7777, lifecycle.getPort().get());
		assertFalse("Lifecycle handled containerInit", lifecycle.isContainerInitHandled());
		assertTrue("Lifecycle didn't handle contextRefreshed", lifecycle.isContextRefreshedHandled());
	}

	@Test(expected = IllegalStateException.class)
	public void portsFailIfNotSet() {
		new AnnotationConfigApplicationContext(Config.class);
	}

	@ImportAutoConfiguration({PropertyPlaceholderAutoConfiguration.class})
	@Configuration
	public static class Config {
		@Bean
		public TestDiscoveryLifecycle testDiscoveryLifecycle() {
			return new TestDiscoveryLifecycle();
		}
	}
}
