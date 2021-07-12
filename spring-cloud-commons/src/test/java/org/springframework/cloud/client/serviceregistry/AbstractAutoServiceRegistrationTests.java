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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
// @checkstyle:off
@SpringBootTest(classes = AbstractAutoServiceRegistrationTests.Config.class, properties = "management.port=0",
		webEnvironment = RANDOM_PORT)
// @checkstyle:on
public class AbstractAutoServiceRegistrationTests {

	@Autowired
	public PostEventListener postEventListener;

	@Autowired
	private TestAutoServiceRegistration autoRegistration;

	@Autowired
	private PreEventListener preEventListener;

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	public void portsWork() {
		then(this.autoRegistration.getPort().get()).isNotEqualTo(0).as("Lifecycle port is zero");
		then(this.managementPort).isNotEqualTo(this.autoRegistration.getPort().get())
				.as("Lifecycle port is management port");
		then(this.port).isEqualTo(this.autoRegistration.getPort().get()).as("Lifecycle port is wrong");
		then(this.autoRegistration.isRunning()).isTrue().as("Lifecycle not running");
		then(this.autoRegistration.getServiceRegistry()).isInstanceOf(TestServiceRegistry.class)
				.as("ServiceRegistry is wrong type");
		TestServiceRegistry serviceRegistry = (TestServiceRegistry) this.autoRegistration.getServiceRegistry();
		then(serviceRegistry.isRegistered()).isTrue().as("Lifecycle not registered");
		then(this.autoRegistration.getAppName()).as("Lifecycle appName is wrong").isEqualTo("application");
	}

	@Test
	public void eventsFireTest() {
		then(this.preEventListener.wasFired).isTrue();
		then(this.preEventListener.registration.getServiceId()).isEqualTo("testRegistration2");
		then(this.postEventListener.wasFired).isTrue();
		then(this.postEventListener.config.getServiceId()).isEqualTo("testRegistration2");
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		public TestAutoServiceRegistration testAutoServiceRegistration() {
			return new TestAutoServiceRegistration();
		}

		@Bean
		public PreEventListener preRegisterListener() {
			return new PreEventListener();
		}

		@Bean
		public PostEventListener postEventListener() {
			return new PostEventListener();
		}

	}

	public static class PreEventListener implements ApplicationListener<InstancePreRegisteredEvent> {

		public boolean wasFired = false;

		public Registration registration;

		@Override
		public void onApplicationEvent(InstancePreRegisteredEvent event) {
			this.registration = event.getRegistration();
			this.wasFired = true;
		}

	}

	public static class PostEventListener implements ApplicationListener<InstanceRegisteredEvent> {

		public boolean wasFired = false;

		public Registration config;

		@Override
		public void onApplicationEvent(InstanceRegisteredEvent event) {
			this.config = (Registration) event.getConfig();
			this.wasFired = true;
		}

	}

	public static class TestRegistration implements Registration {

		@Override
		public String getInstanceId() {
			return "testRegistrationInstance2";
		}

		@Override
		public String getServiceId() {
			return "testRegistration2";
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
			return "testMgmtRegistration2";
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

	public static class TestAutoServiceRegistration extends AbstractAutoServiceRegistration<TestRegistration> {

		private int port = 0;

		public TestAutoServiceRegistration(AutoServiceRegistrationProperties properties) {
			super(null, properties);
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

		protected int getConfiguredPort() {
			return this.port;
		}

		protected void setConfiguredPort(int port) {
			this.port = port;
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
			return getRegistration();
		}

		@Override
		protected boolean isEnabled() {
			return true;
		}

	}

}
