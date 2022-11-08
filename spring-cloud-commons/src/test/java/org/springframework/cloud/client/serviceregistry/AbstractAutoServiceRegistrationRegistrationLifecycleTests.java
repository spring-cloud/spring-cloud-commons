/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.serviceregistry;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Zen Huifer
 */
@SpringBootTest(classes = AbstractAutoServiceRegistrationRegistrationLifecycleTests.Config.class)
class AbstractAutoServiceRegistrationRegistrationLifecycleTests {

	@Autowired
	@Qualifier("testAutoServiceRegistration")
	private TestAutoServiceRegistrationLifecycle autoRegistration;

	@Test
	void startTest() {
		autoRegistration.start();
	}

	@Test
	void stopTest() {
		autoRegistration.stop();
	}

	static class RegistrationLifecycleImpl implements RegistrationLifecycle<TestRegistrationLifecycleRegistration> {

		@Override
		public void postProcessBeforeStartRegister(TestRegistrationLifecycleRegistration registration) {
			registration.getMetadata().putIfAbsent("test_data", "test_data");
			registration.getMetadata().putIfAbsent("is_start", "false");
		}

		@Override
		public void postProcessAfterStartRegister(TestRegistrationLifecycleRegistration registration) {
			assertThat(registration.getMetadata()).containsKey("test_data");
			assertThat(registration.getMetadata().get("test_data")).isEqualTo("test_data");
			assertThat(registration.getMetadata()).containsKey("is_start");
			registration.getMetadata().putIfAbsent("is_start", "true");
		}

		@Override
		public void postProcessBeforeStopRegister(TestRegistrationLifecycleRegistration registration) {
			assertThat(registration.getMetadata().get("is_start")).isEqualTo("true");
			Map<String, String> metadata = registration.getMetadata();
			metadata.putIfAbsent("is_stop", "false");
			metadata.putIfAbsent("is_start", "false");
		}

		@Override
		public void postProcessAfterStopRegister(TestRegistrationLifecycleRegistration registration) {

			assertThat(registration.getMetadata().get("test_data")).isEqualTo("test_data");
			assertThat(registration.getMetadata().get("is_stop")).isEqualTo("false");
			assertThat(registration.getMetadata().get("is_start")).isEqualTo("false");
			registration.getMetadata().putIfAbsent("is_stop", "true");
		}

	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		public TestAutoServiceRegistrationLifecycle testAutoServiceRegistration(
				AutoServiceRegistrationProperties properties) {
			List<RegistrationLifecycle<TestRegistrationLifecycleRegistration>> registrationLifecycles = new ArrayList<>();
			registrationLifecycles.add(new RegistrationLifecycleImpl());
			return new TestAutoServiceRegistrationLifecycle(properties, registrationLifecycles);
		}

	}

	static class TestAutoServiceRegistrationLifecycle
			extends AbstractAutoServiceRegistration<TestRegistrationLifecycleRegistration> {

		private final int port = 0;

		TestRegistrationLifecycleRegistration testRegistrationLifecycleRegistration = new TestRegistrationLifecycleRegistration();

		protected TestAutoServiceRegistrationLifecycle() {
			super(new TestRegistrationLifecycleServiceRegistration(), new AutoServiceRegistrationProperties());
		}

		TestAutoServiceRegistrationLifecycle(AutoServiceRegistrationProperties properties) {
			super(new TestRegistrationLifecycleServiceRegistration(), properties);
		}

		TestAutoServiceRegistrationLifecycle(AutoServiceRegistrationProperties properties,
				List<RegistrationManagementLifecycle<TestRegistrationLifecycleRegistration>> registrationManagementLifecycles,
				List<RegistrationLifecycle<TestRegistrationLifecycleRegistration>> registrationLifecycles) {
			super(new TestRegistrationLifecycleServiceRegistration(), properties, registrationManagementLifecycles,
					registrationLifecycles);
		}

		TestAutoServiceRegistrationLifecycle(AutoServiceRegistrationProperties properties,
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

	static class TestRegistrationLifecycleServiceRegistration
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

	static class TestRegistrationLifecycleRegistration implements Registration {

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
