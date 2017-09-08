package org.springframework.cloud.client.serviceregistry.endpoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceRegistryEndpointTests.TestConfiguration.class)
public class ServiceRegistryEndpointTests {
	private static final String UPDATED_STATUS = "updatedstatus";
	private static final String MYSTATUS = "mystatus";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private TestServiceRegistry serviceRegistry;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void testGet() throws Exception {
		this.mvc.perform(get("/application/service-registry/instance-status")).andExpect(status().isOk())
				.andExpect(content().string(containsString(MYSTATUS)));
	}

	@Test
	public void testPost() throws Exception {
		this.mvc.perform(post("/application/service-registry/instance-status").content(UPDATED_STATUS)).andExpect(status().isOk());
		assertThat(this.serviceRegistry.getUpdatedStatus().get()).isEqualTo(UPDATED_STATUS);
	}

	@Import({JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebMvcAutoConfiguration.class,
			// ManagementServerPropertiesAutoConfiguration.class
	})
	@Configuration
	public static class TestConfiguration {
		@Bean
		Registration registration() {
			return new Registration() {
				@Override
				public String getServiceId() {
					return "testRegistration1";
				}
			};
		}

		@Bean
		ServiceRegistryEndpoint serviceRegistryEndpoint(Registration reg) {
			ServiceRegistryEndpoint endpoint = new ServiceRegistryEndpoint(serviceRegistry());
			endpoint.setRegistration(reg);
			return endpoint;
		}

		@Bean
		ServiceRegistry serviceRegistry() {
			return new TestServiceRegistry() ;
		}
	}

	static class TestServiceRegistry implements ServiceRegistry {

		AtomicReference<String> updatedStatus = new AtomicReference<>();

		@Override
		public void register(Registration registration) {

		}

		@Override
		public void deregister(Registration registration) {

		}

		@Override
		public void close() {

		}

		@Override
		public void setStatus(Registration registration, String status) {
			updatedStatus.compareAndSet(null, status);
		}

		@Override
		public Object getStatus(Registration registration) {
			return MYSTATUS;
		}

		public AtomicReference<String> getUpdatedStatus() {
			return updatedStatus;
		}
	}
}
