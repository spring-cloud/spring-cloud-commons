package org.springframework.cloud.client.serviceregistry.endpoint;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceRegistryEndpointTests.TestConfiguration.class, properties = "management.endpoints.web.expose=*")
public class ServiceRegistryEndpointTests {
	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();
	
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
		this.mvc.perform(get(BASE_PATH + "/service-registry")).andExpect(status().isOk())
				.andExpect(content().string(containsString(MYSTATUS)));
	}

	@Test
	public void testPost() throws Exception {
		Map<String, String> status = Collections.singletonMap("status", UPDATED_STATUS);
		this.mvc.perform(post(BASE_PATH + "/service-registry")
				.content(new ObjectMapper().writeValueAsString(status))
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		assertThat(this.serviceRegistry.getUpdatedStatus().get()).isEqualTo(UPDATED_STATUS);
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	public static class TestConfiguration {
		@Bean
		Registration registration() {
			return new Registration() {
				@Override
				public String getServiceId() {
					return "testRegistration1";
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
			};
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
