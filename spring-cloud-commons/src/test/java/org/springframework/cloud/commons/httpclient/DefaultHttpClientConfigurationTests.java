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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
@SpringBootTest(classes = MyApplication.class, properties = { "spring.cloud.httpclient.ok.enabled: true" })
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
		then(ApacheHttpClientConnectionManagerFactory.class.isInstance(this.connectionManagerFactory)).isTrue();
		then(DefaultApacheHttpClientConnectionManagerFactory.class.isInstance(this.connectionManagerFactory)).isTrue();
	}

	@Test
	public void apacheHttpClientFactory() throws Exception {
		then(ApacheHttpClientFactory.class.isInstance(this.httpClientFactory)).isTrue();
		then(DefaultApacheHttpClientFactory.class.isInstance(this.httpClientFactory)).isTrue();
	}

	@Test
	public void connPoolFactory() throws Exception {
		then(OkHttpClientConnectionPoolFactory.class.isInstance(this.okHttpClientConnectionPoolFactory)).isTrue();
		then(DefaultOkHttpClientConnectionPoolFactory.class.isInstance(this.okHttpClientConnectionPoolFactory))
				.isTrue();
	}

	@Test
	public void setOkHttpClientFactory() throws Exception {
		then(OkHttpClientFactory.class.isInstance(this.okHttpClientFactory)).isTrue();
		then(DefaultOkHttpClientFactory.class.isInstance(this.okHttpClientFactory)).isTrue();
	}

}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class MyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

}
