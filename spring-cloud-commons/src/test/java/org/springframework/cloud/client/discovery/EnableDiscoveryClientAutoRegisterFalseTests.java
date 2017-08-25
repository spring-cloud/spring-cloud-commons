package org.springframework.cloud.client.discovery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class EnableDiscoveryClientAutoRegisterFalseTests {

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
		assertNull(autoConfiguration);
		assertNull(autoServiceRegistration);
		assertNull(autoServiceRegistrationProperties);
		assertFalse(autoRegisterProperty);
	}


	@EnableAutoConfiguration
	@Configuration
	@EnableDiscoveryClient(autoRegister = false)
	public static class App {
	}
}
