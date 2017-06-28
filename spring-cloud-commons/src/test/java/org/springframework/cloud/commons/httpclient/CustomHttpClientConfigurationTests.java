package org.springframework.cloud.commons.httpclient;

import java.util.concurrent.TimeUnit;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CustomApplication.class)
public class CustomHttpClientConfigurationTests {

	@Autowired
	ApacheHttpClientFactory httpClientFactory;

	@Autowired
	ApacheHttpClientConnectionManagerFactory connectionManagerFactory;

	@Test
	public void connManFactory() throws Exception {
		assertTrue(ApacheHttpClientConnectionManagerFactory.class
				.isInstance(connectionManagerFactory));
		assertTrue(CustomApplication.MyApacheHttpClientConnectionManagerFactory.class
				.isInstance(connectionManagerFactory));
	}

	@Test
	public void apacheHttpClientFactory() throws Exception {
		assertTrue(ApacheHttpClientFactory.class.isInstance(httpClientFactory));
		assertTrue(CustomApplication.MyApacheHttpClientFactory.class
				.isInstance(httpClientFactory));
	}

}

@Configuration
@EnableAutoConfiguration
class CustomApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

	@Configuration
	static class MyConfig {

		@Bean
		public ApacheHttpClientFactory clientFactory() {
			return new MyApacheHttpClientFactory();
		}

		@Bean
		ApacheHttpClientConnectionManagerFactory connectionManagerFactory() {
			return new MyApacheHttpClientConnectionManagerFactory();
		}

	}

	static class MyApacheHttpClientFactory implements ApacheHttpClientFactory {

		@Override
		public CloseableHttpClient createClient(RequestConfig requestConfig,
				HttpClientConnectionManager connectionManager) {
			return null;
		}
	}

	static class MyApacheHttpClientConnectionManagerFactory
			implements ApacheHttpClientConnectionManagerFactory {

		@Override
		public HttpClientConnectionManager newConnectionManager(
				boolean disableSslValidation, int maxTotalConnections,
				int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit,
				RegistryBuilder registryBuilder) {
			return null;
		}
	}
}
