package org.springframework.cloud.commons.httpclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MyApplication.class)
public class DefaultHttpClientConfigurationTests {
	@Autowired
	ApacheHttpClientFactory httpClientFactory;

	@Autowired
	ApacheHttpClientConnectionManagerFactory connectionManagerFactory;

	@Test
	public void connManFactory() throws Exception {
		assertTrue(ApacheHttpClientConnectionManagerFactory.class.isInstance(connectionManagerFactory));
		assertTrue(DefaultApacheHttpClientConnectionManagerFactory.class.isInstance(connectionManagerFactory));
	}

	@Test
	public void apacheHttpClientFactory() throws Exception {
		assertTrue(ApacheHttpClientFactory.class.isInstance(httpClientFactory));
		assertTrue(DefaultApacheHttpClientFactory.class.isInstance(httpClientFactory));
	}
}

@Configuration
@EnableAutoConfiguration
class MyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}
}