package org.springframework.cloud.commons.httpclient;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MyApplication.class, properties = {"spring.cloud.httpclient.ok.enabled: true"})
public class DefaultHttpClientConfigurationTests {
	@Autowired
	ApacheHttpClientFactory httpClientFactory;

	@Autowired
	ApacheHttpClientConnectionManagerFactory connectionManagerFactory;

	@Autowired
	OkHttpClientFactory okHttpClientFactory;

	@Autowired
	OkHttpClientConnectionPoolFactory okHttpClientConnectionPoolFactory;

	@Test
	public void connManFactory() throws Exception {
		assertTrue(ApacheHttpClientConnectionManagerFactory.class
			.isInstance(this.connectionManagerFactory));
		assertTrue(DefaultApacheHttpClientConnectionManagerFactory.class
			.isInstance(this.connectionManagerFactory));
	}

	@Test
	public void apacheHttpClientFactory() throws Exception {
		assertTrue(ApacheHttpClientFactory.class.isInstance(this.httpClientFactory));
		assertTrue(DefaultApacheHttpClientFactory.class.isInstance(this.httpClientFactory));
	}

	@Test
	public void connPoolFactory() throws Exception {
		assertTrue(OkHttpClientConnectionPoolFactory.class
			.isInstance(this.okHttpClientConnectionPoolFactory));
		assertTrue(DefaultOkHttpClientConnectionPoolFactory.class
			.isInstance(this.okHttpClientConnectionPoolFactory));
	}

	@Test
	public void setOkHttpClientFactory() throws Exception {
		assertTrue(OkHttpClientFactory.class.isInstance(this.okHttpClientFactory));
		assertTrue(DefaultOkHttpClientFactory.class.isInstance(this.okHttpClientFactory));
	}
}

@Configuration
@EnableAutoConfiguration
class MyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}
}
