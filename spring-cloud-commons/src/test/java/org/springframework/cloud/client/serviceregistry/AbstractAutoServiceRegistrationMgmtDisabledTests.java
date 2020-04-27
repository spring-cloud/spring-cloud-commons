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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AbstractAutoServiceRegistrationMgmtDisabledTests.Config.class,
		properties = { "management.port=0",
				"spring.cloud.service-registry.auto-registration.register-management=false" },
		webEnvironment = RANDOM_PORT)
public class AbstractAutoServiceRegistrationMgmtDisabledTests {

	@Autowired
	private TestAutoServiceRegistration autoRegistration;

	@Test
	public void portsWork() {
		BDDAssertions.then(this.autoRegistration.shouldRegisterManagement()).isFalse();
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		public TestAutoServiceRegistration testAutoServiceRegistration(
				AutoServiceRegistrationProperties properties) {
			return new TestAutoServiceRegistration(properties);
		}

	}

	public static class TestRegistration implements Registration {

		@Override
		public String getInstanceId() {
			return "testRegistrationInstance3";
		}

		@Override
		public String getServiceId() {
			return "testRegistration3";
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
			return null;
		}

	}

	public static class TestMgmtRegistration extends TestRegistration {

		@Override
		public String getServiceId() {
			return "testMgmtRegistration3";
		}

	}

	public static class TestServiceRegistry implements ServiceRegistry<TestRegistration> {

		private boolean registered = false;

		private boolean deregistered = false;

		@Override
		public void register(TestRegistration registration) {
			if (registration == null) {
				throw new NullPointerException();
			}
			if (!(registration instanceof TestMgmtRegistration)) {
				this.registered = true;
			}
		}

		@Override
		public void deregister(TestRegistration registration) {
			if (registration == null) {
				throw new NullPointerException();
			}
			if (!(registration instanceof TestMgmtRegistration)) {
				this.deregistered = true;
			}
		}

		@Override
		public void close() {
		}

		@Override
		public void setStatus(TestRegistration registration, String status) {
			// TODO: test setStatus
		}

		@Override
		public Object getStatus(TestRegistration registration) {
			// TODO: test getStatus
			return null;
		}

		boolean isRegistered() {
			return this.registered;
		}

		boolean isDeregistered() {
			return this.deregistered;
		}

	}

	public static class TestAutoServiceRegistration
			extends AbstractAutoServiceRegistration<TestRegistration> {

		private int port = 0;

		public TestAutoServiceRegistration(AutoServiceRegistrationProperties properties) {
			super(new TestServiceRegistry(), properties);
		}

		protected TestAutoServiceRegistration() {
			super(new TestServiceRegistry());
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
		protected TestRegistration getRegistration() {
			return new TestRegistration();
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
