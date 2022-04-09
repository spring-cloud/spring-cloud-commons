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

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ServiceInstanceMetadataHandler} test class.
 *
 * @author huifer
 */
public class ServiceInstanceMetadataHandlerTests {

	@Test
	public void tt() {
		String[] args = new String[] { "--spring.cloud.discovery.metadata.t1=t1",
				"--spring.cloud.discovery.metadata.t2=t2"

		};

		ConfigurableApplicationContext run = SpringApplication.run(Config.class, args);
		TestMetaDataRegistration bean = run.getBean(TestMetaDataRegistration.class);
		MetaDataRegistration registration = bean.getRegistration();
		Map<String, String> metadata = registration.getMetadata();
		assertThat(metadata).containsKey("t1");
		assertThat(metadata.get("t1")).isEqualTo("t1");

		assertThat(metadata).containsKey("t2");
		assertThat(metadata.get("t2")).isEqualTo("t2");

	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AutoServiceRegistrationMetadataProperties.class)
	public static class Config {

		@Bean
		public ServiceInstanceMetadataHandler serviceInstanceMetadataHandler() {
			return new DefaultServiceInstanceMetadataHandler();
		}

		@Bean
		public TestMetaDataRegistration testAutoServiceRegistration(AutoServiceRegistrationProperties properties,
				ServiceInstanceMetadataHandler serviceInstanceMetadataHandler

		) {
			List<RegistrationLifecycle<MetaDataRegistration>> registrationLifecycles = new ArrayList<>();
			RegistrationLifecycleImpl registrationLifecycle = new RegistrationLifecycleImpl();
			registrationLifecycle.addServiceInstanceMetadataHandler(serviceInstanceMetadataHandler);
			registrationLifecycles.add(registrationLifecycle);
			return new TestMetaDataRegistration(properties, registrationLifecycles);
		}

	}

	public static class RegistrationLifecycleImpl implements RegistrationLifecycle<MetaDataRegistration> {

		List<ServiceInstanceMetadataHandler> metadataHandlers = new ArrayList<>();

		public void addServiceInstanceMetadataHandler(ServiceInstanceMetadataHandler handler) {
			this.metadataHandlers.add(handler);
		}

		@Override
		public void postProcessBeforeStartRegister(MetaDataRegistration registration) {
			for (ServiceInstanceMetadataHandler metadataHandler : metadataHandlers) {
				metadataHandler.settingMetaData(registration);

			}
		}

		@Override
		public void postProcessAfterStartRegister(MetaDataRegistration registration) {

		}

		@Override
		public void postProcessBeforeStopRegister(MetaDataRegistration registration) {

		}

		@Override
		public void postProcessAfterStopRegister(MetaDataRegistration registration) {

		}

		@Override
		public int getOrder() {
			return RegistrationLifecycle.super.getOrder();
		}

	}

	public static class TestMetaDataRegistration extends AbstractAutoServiceRegistration<MetaDataRegistration> {

		MetaDataRegistration metaDataRegistration = new MetaDataRegistration();

		protected TestMetaDataRegistration() {
			super(new MetaDataRegistrationRegistration());
		}

		public TestMetaDataRegistration(AutoServiceRegistrationProperties properties) {
			super(new MetaDataRegistrationRegistration(), properties);
		}

		public TestMetaDataRegistration(AutoServiceRegistrationProperties properties,
				List<RegistrationManagementLifecycle<MetaDataRegistration>> registrationManagementLifecycles,
				List<RegistrationLifecycle<MetaDataRegistration>> registrationLifecycles) {
			super(new MetaDataRegistrationRegistration(), properties, registrationManagementLifecycles,
					registrationLifecycles);
		}

		public TestMetaDataRegistration(AutoServiceRegistrationProperties properties,
				List<RegistrationLifecycle<MetaDataRegistration>> registrationLifecycles) {
			super(new MetaDataRegistrationRegistration(), properties, registrationLifecycles);
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
		protected MetaDataRegistration getRegistration() {
			return metaDataRegistration;
		}

		@Override
		protected MetaDataRegistration getManagementRegistration() {
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

	public static class MetaDataRegistrationRegistration implements ServiceRegistry<MetaDataRegistration> {

		@Override
		public void register(MetaDataRegistration registration) {

		}

		@Override
		public void deregister(MetaDataRegistration registration) {

		}

		@Override
		public void close() {

		}

		@Override
		public void setStatus(MetaDataRegistration registration, String status) {

		}

		@Override
		public <T> T getStatus(MetaDataRegistration registration) {
			return null;
		}

	}

	public static class MetaDataRegistration implements Registration {

		private final Map<String, String> metadata = new HashMap<>(8);

		@Override
		public String getInstanceId() {
			return "ServiceInstanceMetadataHandlerTests";
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
