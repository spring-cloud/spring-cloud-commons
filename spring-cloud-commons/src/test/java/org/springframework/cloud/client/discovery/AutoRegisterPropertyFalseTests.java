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

package org.springframework.cloud.client.discovery;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
@SpringBootTest(properties = { "spring.cloud.service-registry.auto-registration.enabled: false" })
public class AutoRegisterPropertyFalseTests {

	@Autowired(required = false)
	AutoServiceRegistrationAutoConfiguration autoConfiguration;

	@Autowired(required = false)
	AutoServiceRegistration autoServiceRegistration;

	@Autowired(required = false)
	AutoServiceRegistrationProperties autoServiceRegistrationProperties;

	@Value("${spring.cloud.service-registry.auto-registration.enabled}")
	Boolean autoRegisterProperty;

	@Test
	public void veryifyBeans() {
		then(this.autoConfiguration).isNull();
		then(this.autoServiceRegistration).isNull();
		then(this.autoServiceRegistrationProperties).isNull();
		then(this.autoRegisterProperty).isFalse();
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class App {

	}

}
