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

package org.springframework.cloud.commons.httpclient;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
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

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CustomApplication.class,
		properties = { "spring.cloud.httpclient.ok.enabled: true" })
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
		then(ApacheHttpClientConnectionManagerFactory.class
				.isInstance(this.connectionManagerFactory)).isTrue();
		then(CustomApplication.MyApacheHttpClientConnectionManagerFactory.class
				.isInstance(this.connectionManagerFactory)).isTrue();
	}

	@Test
	public void apacheHttpClientFactory() throws Exception {
		then(ApacheHttpClientFactory.class.isInstance(this.httpClientFactory)).isTrue();
		then(CustomApplication.MyApacheHttpClientFactory.class
				.isInstance(this.httpClientFactory)).isTrue();
	}

	@Test
	public void connectionPoolFactory() throws Exception {
		then(OkHttpClientConnectionPoolFactory.class
				.isInstance(this.okHttpClientConnectionPoolFactory)).isTrue();
		then(CustomApplication.MyOkHttpConnectionPoolFactory.class
				.isInstance(this.okHttpClientConnectionPoolFactory)).isTrue();
	}

	@Test
	public void okHttpClientFactory() throws Exception {
		then(OkHttpClientFactory.class.isInstance(this.okHttpClientFactory)).isTrue();
		then(CustomApplication.MyOkHttpClientFactory.class
				.isInstance(this.okHttpClientFactory)).isTrue();
	}

}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class CustomApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

	@Configuration(proxyBeanMethods = false)
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

	static class MyOkHttpConnectionPoolFactory
			implements OkHttpClientConnectionPoolFactory {

		@Override
		public ConnectionPool create(int maxIdleConnections, long keepAliveDuration,
				TimeUnit timeUnit) {
			return null;
		}

	}

}
