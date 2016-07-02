package org.springframework.cloud.client.discovery;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
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
		assertThat("ServiceRegistry is wrong type", lifecycle.getServiceRegistry(), is(instanceOf(TestServiceRegistry.class)));
		TestServiceRegistry serviceRegistry = (TestServiceRegistry) lifecycle.getServiceRegistry();
		assertTrue("Lifecycle not registered", serviceRegistry.isRegistered());
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

	public static class TestRegistration {
	}

	public static class TestServiceRegistry implements ServiceRegistry<TestRegistration> {
		private boolean registered = false;
		private boolean deregistered = false;

		@Override
		public void register(TestRegistration registration) {
			this.registered = true;
		}

		@Override
		public void deregister(TestRegistration registration) {
			this.deregistered = true;
		}

		public boolean isRegistered() {
			return registered;
		}

		public boolean isDeregistered() {
			return deregistered;
		}
	}

	public static class TestDiscoveryLifecycle extends AbstractDiscoveryLifecycle<TestRegistration> {
		private int port = 0;
		private boolean registered = false;
		private boolean deregistered = false;

		protected TestDiscoveryLifecycle() {
			super(new TestServiceRegistry());
		}

		@Override
		protected int getConfiguredPort() {
			return port;
		}

		@Override
		protected void setConfiguredPort(int port) {
			this.port = port;
		}

		@Override
		protected TestRegistration getRegistration() {
			return null;
		}

		@Override
		protected TestRegistration getManagementRegistration() {
			return null;
		}

		@Override
		protected boolean isEnabled() {
			return true;
		}

	}
}
