package org.springframework.cloud.client.discovery;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AbstractDiscoveryLifecycleTests.Config.class)
@WebIntegrationTest(randomPort = true, value = "management.port=0")
public class AbstractDiscoveryLifecycleTests {

	@Autowired
	private TestDiscoveryLifecycle lifecycle;

	@Value("${local.server.port}")
	private int port;

	@Value("${local.management.port}")
	private int managementPort;

	@Test
	public void portsWork() {
		assertNotEquals("Lifecycle port is zero", 0, lifecycle.getPort().get());
		assertNotEquals("Lifecycle port is management port", managementPort, lifecycle.getPort().get());
		assertEquals("Lifecycle port is wrong", port, lifecycle.getPort().get());
		assertTrue("Lifecycle not running", lifecycle.isRunning());
		assertTrue("Lifecycle not registered", lifecycle.isRegistered());
		assertEquals("Lifecycle appName is wrong", "application", lifecycle.getAppName());
	}

	@EnableAutoConfiguration
	@Configuration
	public static class Config {
		@Bean
		public TestDiscoveryLifecycle testDiscoveryLifecycle() {
			return new TestDiscoveryLifecycle();
		}
	}

	public static class TestDiscoveryLifecycle extends AbstractDiscoveryLifecycle {
		private int port = 0;
		private boolean registered = false;
		private boolean deregistered = false;

		@Override
		protected int getConfiguredPort() {
			return port;
		}

		@Override
		protected void setConfiguredPort(int port) {
			this.port = port;
		}

		@Override
		protected Object getConfiguration() {
			return this;
		}

		@Override
		protected void register() {
			this.registered = true;
		}

		@Override
		protected void deregister() {
			this.deregistered = true;
		}

		@Override
		protected boolean isEnabled() {
			return true;
		}

		public boolean isRegistered() {
			return registered;
		}

		public boolean isDeregistered() {
			return deregistered;
		}
	}
}
