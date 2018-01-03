package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
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
@SpringBootTest(classes = CustomApplication.class, properties = {"spring.cloud.httpclient.ok.enabled: true"})
public class CustomHttpClientConfigurationTests {

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

	@Test
	public void connectionPoolFactory() throws Exception {
		assertTrue(OkHttpClientConnectionPoolFactory.class.isInstance(okHttpClientConnectionPoolFactory));
		assertTrue(CustomApplication.MyOkHttpConnectionPoolFactory.class.isInstance(okHttpClientConnectionPoolFactory));
	}

	@Test
	public void okHttpClientFactory() throws Exception {
		assertTrue(OkHttpClientFactory.class.isInstance(okHttpClientFactory));
		assertTrue(CustomApplication.MyOkHttpClientFactory.class.isInstance(okHttpClientFactory));
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
		public ApacheHttpClientConnectionManagerFactory connectionManagerFactory() {
			return new MyApacheHttpClientConnectionManagerFactory();
		}

		@Bean
		public OkHttpClientConnectionPoolFactory connectionPoolFactory() {
			return new MyOkHttpConnectionPoolFactory();
		}

		@Bean
		public OkHttpClientFactory okHttpClientFactory() {
			return new MyOkHttpClientFactory();
		}

	}

	static class MyApacheHttpClientFactory implements ApacheHttpClientFactory {

		@Override
		public HttpClientBuilder createBuilder() {
			return HttpClientBuilder.create();
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

	static class MyOkHttpClientFactory implements OkHttpClientFactory {
		@Override
		public OkHttpClient.Builder createBuilder(boolean disableSslValidation) {
			return new OkHttpClient.Builder();
		}
	}

	static class MyOkHttpConnectionPoolFactory implements OkHttpClientConnectionPoolFactory {

		@Override
		public ConnectionPool create(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
			return null;
		}
	}
}
