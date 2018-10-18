package org.springframework.cloud.client.serviceregistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
public class AutoServiceRegistrationAutoConfigurationTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void hasAutoServiceRegistration() {
		try(AnnotationConfigApplicationContext context = setup(HasAutoServiceRegistrationConfiguration.class)) {
			AutoServiceRegistration autoServiceRegistration = context.getBean(AutoServiceRegistration.class);
			assertThat(autoServiceRegistration).isNotNull();
		}
	}

	@Test
	public void noAutoServiceRegistrationAndFailFast() {
		this.exception.expect(BeanCreationException.class);
		this.exception.expectMessage(Matchers.containsString("no AutoServiceRegistration"));
		try(AnnotationConfigApplicationContext context = setup("spring.cloud.service-registry.auto-registration.failFast=true")) {
			assertNoBean(context);
		}
	}

	@Test
	public void noAutoServiceRegistrationAndFailFastFalse() {
		try(AnnotationConfigApplicationContext context = setup()) {
			assertNoBean(context);
		}
	}

	private void assertNoBean(AnnotationConfigApplicationContext context) {
		Map<String, AutoServiceRegistration> beans = context.getBeansOfType(AutoServiceRegistration.class);
		assertThat(beans).isEmpty();
	}

	@Configuration
	static class HasAutoServiceRegistrationConfiguration {
		@Bean
		public AutoServiceRegistration autoServiceRegistration() {
			return new AutoServiceRegistration() {};
		}
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
}
