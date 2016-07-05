package org.springframework.cloud.client.serviceregistry;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AbstractAutoServiceRegistrationTests.Config.class)
@WebIntegrationTest(randomPort = true, value = "management.port=0")
public class AbstractAutoServiceRegistrationTests {

	@Autowired
	private TestAutoServiceRegistration autoRegistration;

	@Value("${local.server.port}")
	private int port;

	@Value("${local.management.port}")
	private int managementPort;

	@Test
	public void portsWork() {
		assertNotEquals("Lifecycle port is zero", 0, autoRegistration.getPort().get());
		assertNotEquals("Lifecycle port is management port", managementPort, autoRegistration.getPort().get());
		assertEquals("Lifecycle port is wrong", port, autoRegistration.getPort().get());
		assertTrue("Lifecycle not running", autoRegistration.isRunning());
		assertThat("ServiceRegistry is wrong type", autoRegistration.getServiceRegistry(), is(instanceOf(TestServiceRegistry.class)));
		TestServiceRegistry serviceRegistry = (TestServiceRegistry) autoRegistration.getServiceRegistry();
		assertTrue("Lifecycle not registered", serviceRegistry.isRegistered());
		assertEquals("Lifecycle appName is wrong", "application", autoRegistration.getAppName());
	}

	@EnableAutoConfiguration
	@Configuration
	public static class Config {
		@Bean
		public TestAutoServiceRegistration testAutoServiceRegistration() {
			return new TestAutoServiceRegistration();
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

		@Override
		public void close() { }

		boolean isRegistered() {
			return registered;
		}

		boolean isDeregistered() {
			return deregistered;
		}
	}

	public static class TestAutoServiceRegistration extends AbstractAutoServiceRegistration<TestRegistration> {
		private int port = 0;

		@Override
		protected AtomicInteger getPort() {
			return super.getPort();
		}

		@Override
		protected String getAppName() {
			return super.getAppName();
		}

		protected TestAutoServiceRegistration() {
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
		protected Object getConfiguration() {
			return null;
		}

		@Override
		protected boolean isEnabled() {
			return true;
		}


	}
}
