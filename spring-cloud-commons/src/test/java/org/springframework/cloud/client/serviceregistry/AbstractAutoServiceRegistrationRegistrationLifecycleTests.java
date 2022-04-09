package org.springframework.cloud.client.serviceregistry;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AbstractAutoServiceRegistrationRegistrationLifecycleTests
 *
 * @author huifer
 */
@SpringBootTest(classes = AbstractAutoServiceRegistrationRegistrationLifecycleTests.Config.class)
public class AbstractAutoServiceRegistrationRegistrationLifecycleTests {

	@Autowired
	@Qualifier("testAutoServiceRegistration")
	private TestAutoServiceRegistrationLifecycle autoRegistration;

	@Test
	public void startTest() {
		autoRegistration.start();
	}

	@Test
	public void stopTest() {
		autoRegistration.stop();
	}

	public static class RegistrationLifecycleImpl
			implements RegistrationLifecycle<TestRegistrationLifecycleRegistration> {

		private static final Logger log = LoggerFactory.getLogger(RegistrationLifecycleImpl.class);

		@Override
		public void postProcessBeforeStartRegister(TestRegistrationLifecycleRegistration registration) {
			registration.getMetadata().putIfAbsent("test_data", "test_data");
			registration.getMetadata().putIfAbsent("is_start", "false");
		}

		@Override
		public void postProcessAfterStartRegister(TestRegistrationLifecycleRegistration registration) {
			Assert.assertTrue(registration.getMetadata().containsKey("test_data"));
			Assert.assertEquals(registration.getMetadata().get("test_data"), "test_data");
			Assert.assertTrue(registration.getMetadata().containsKey("is_start"));
			registration.getMetadata().putIfAbsent("is_start", "true");
		}

		@Override
		public void postProcessBeforeStopRegister(TestRegistrationLifecycleRegistration registration) {
			Assert.assertEquals(registration.getMetadata().get("is_start"), "true");
			registration.getMetadata().putIfAbsent("is_stop", "false");
			registration.getMetadata().putIfAbsent("is_start", "false");
		}

		@Override
		public void postProcessAfterStopRegister(TestRegistrationLifecycleRegistration registration) {
			Assert.assertEquals(registration.getMetadata().get("test_data"), "test_data");
			Assert.assertEquals(registration.getMetadata().get("is_stop"), "false");
			Assert.assertEquals(registration.getMetadata().get("is_start"), "false");
			registration.getMetadata().putIfAbsent("is_stop", "true");
		}

	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class Config {

		@Bean(value = "testAutoServiceRegistration")
		public TestAutoServiceRegistrationLifecycle testAutoServiceRegistration(
				AutoServiceRegistrationProperties properties) {
			List<RegistrationLifecycle<TestRegistrationLifecycleRegistration>> registrationLifecycles = new ArrayList<>();
			registrationLifecycles.add(new RegistrationLifecycleImpl());
			TestAutoServiceRegistrationLifecycle testAutoServiceRegistrationLifecycle = new TestAutoServiceRegistrationLifecycle(
					properties, registrationLifecycles);
			return testAutoServiceRegistrationLifecycle;
		}

	}

	public static class TestAutoServiceRegistrationLifecycle
			extends AbstractAutoServiceRegistration<TestRegistrationLifecycleRegistration> {

		private final int port = 0;

		TestRegistrationLifecycleRegistration testRegistrationLifecycleRegistration = new TestRegistrationLifecycleRegistration();

		protected TestAutoServiceRegistrationLifecycle() {
			super(new TestRegistrationLifecycleServiceRegistration());
		}

		public TestAutoServiceRegistrationLifecycle(AutoServiceRegistrationProperties properties) {
			super(new TestRegistrationLifecycleServiceRegistration(), properties);
		}

		public TestAutoServiceRegistrationLifecycle(AutoServiceRegistrationProperties properties,
				List<RegistrationManagementLifecycle<TestRegistrationLifecycleRegistration>> registrationManagementLifecycles,
				List<RegistrationLifecycle<TestRegistrationLifecycleRegistration>> registrationLifecycles) {
			super(new TestRegistrationLifecycleServiceRegistration(), properties, registrationManagementLifecycles,
					registrationLifecycles);
		}

		public TestAutoServiceRegistrationLifecycle(AutoServiceRegistrationProperties properties,
				List<RegistrationLifecycle<TestRegistrationLifecycleRegistration>> registrationLifecycles) {
			super(new TestRegistrationLifecycleServiceRegistration(), properties, registrationLifecycles);
		}

		@Override
		protected AtomicInteger getPort() {
			return super.getPort();
		}

		@Override
		protected String getAppName() {
			return super.getAppName();
		}

		@Override
		protected TestRegistrationLifecycleRegistration getRegistration() {
			return testRegistrationLifecycleRegistration;
		}

		@Override
		protected TestRegistrationLifecycleRegistration getManagementRegistration() {
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

	public static class TestRegistrationLifecycleServiceRegistration
			implements ServiceRegistry<TestRegistrationLifecycleRegistration> {

		private boolean registered = false;

		private boolean deregistered = false;

		@Override
		public void register(TestRegistrationLifecycleRegistration registration) {
			if (registration == null) {
				throw new NullPointerException();
			}
			if (!(registration instanceof TestRegistrationLifecycleRegistration)) {
				this.registered = true;
			}
		}

		@Override
		public void deregister(TestRegistrationLifecycleRegistration registration) {
			if (registration == null) {
				throw new NullPointerException();
			}
			if (!(registration instanceof TestRegistrationLifecycleRegistration)) {
				this.deregistered = true;
			}
		}

		@Override
		public void close() {

		}

		@Override
		public void setStatus(TestRegistrationLifecycleRegistration registration, String status) {

		}

		@Override
		public <T> T getStatus(TestRegistrationLifecycleRegistration registration) {
			return null;
		}

		boolean isRegistered() {
			return registered;
		}

		boolean isDeregistered() {
			return deregistered;
		}

	}

	public static class TestRegistrationLifecycleRegistration implements Registration {

		private final Map<String, String> metadata = new HashMap<>(8);

		@Override
		public String getInstanceId() {
			return "TestRegistrationLifecycleRegistration";
		}

		@Override
		public String getServiceId() {
			return "test";
		}

		@Override
		public String getHost() {
			return null;
		}

		@Override
		public int getPort() {
			return 0;
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public URI getUri() {
			return null;
		}

		@Override
		public Map<String, String> getMetadata() {
			return this.metadata;
		}

	}

}
