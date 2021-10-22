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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 */
public class AutoServiceRegistrationAutoConfigurationTests {

	@Test
	public void hasAutoServiceRegistration() {
		try (AnnotationConfigApplicationContext context = setup(HasAutoServiceRegistrationConfiguration.class)) {
			AutoServiceRegistration autoServiceRegistration = context.getBean(AutoServiceRegistration.class);
			then(autoServiceRegistration).isNotNull();
		}
	}

	@Test
	public void noAutoServiceRegistrationAndFailFast() {
		assertThatThrownBy(() -> {
			try (AnnotationConfigApplicationContext context = setup(
					"spring.cloud.service-registry.auto-registration.failFast=true")) {
				assertNoBean(context);
			}
		}).isInstanceOf(BeanCreationException.class).hasMessageContaining("no AutoServiceRegistration");
	}

	@Test
	public void noAutoServiceRegistrationAndFailFastFalse() {
		try (AnnotationConfigApplicationContext context = setup()) {
			assertNoBean(context);
		}
	}

	private void assertNoBean(AnnotationConfigApplicationContext context) {
		Map<String, AutoServiceRegistration> beans = context.getBeansOfType(AutoServiceRegistration.class);
		then(beans).isEmpty();
	}

	private AnnotationConfigApplicationContext setup(Class... classes) {
		return setup(null, classes);
	}

	private AnnotationConfigApplicationContext setup(String property, Class... classes) {
		ArrayList<Class> list = new ArrayList<>();
		list.add(AutoServiceRegistrationConfiguration.class);
		list.add(AutoServiceRegistrationAutoConfiguration.class);
		list.addAll(Arrays.asList(classes));
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(list.toArray(new Class[0]));
		if (StringUtils.hasText(property)) {
			TestPropertyValues.of(property).applyTo(context);
		}
		context.refresh();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	static class HasAutoServiceRegistrationConfiguration {

		@Bean
		public AutoServiceRegistration autoServiceRegistration() {
			return new AutoServiceRegistration() {
			};
		}

	}

}
