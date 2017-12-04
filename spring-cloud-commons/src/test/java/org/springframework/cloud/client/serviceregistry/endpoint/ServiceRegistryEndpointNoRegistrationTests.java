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
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpointTests.TestServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceRegistryEndpointNoRegistrationTests.TestConfiguration.class)
public class ServiceRegistryEndpointNoRegistrationTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void testGet() throws Exception {
		this.mvc.perform(get("/service-registry/instance-status")).andExpect(status().isNotFound());
	}

	@Test
	public void testPost() throws Exception {
		this.mvc.perform(post("/service-registry/instance-status").content("newstatus")).andExpect(status().isNotFound());
	}

	@Import({JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebMvcAutoConfiguration.class,
			// ManagementServerPropertiesAutoConfiguration.class
	})
	@Configuration
	public static class TestConfiguration {
		@Bean
		ServiceRegistryEndpoint serviceRegistryEndpoint() {
			return new ServiceRegistryEndpoint(serviceRegistry());
		}

		@Bean
		ServiceRegistry serviceRegistry() {
			return new TestServiceRegistry() ;
		}
	}
}
