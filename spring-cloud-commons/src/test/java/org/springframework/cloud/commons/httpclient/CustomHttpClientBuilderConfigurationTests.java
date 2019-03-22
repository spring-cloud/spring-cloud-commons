/*
 * Copyright 2012-2019 the original author or authors.
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

import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
@SpringBootTest(classes = CustomHttpClientBuilderApplication.class)
public class CustomHttpClientBuilderConfigurationTests {

	@Autowired
	ApacheHttpClientFactory apacheHttpClientFactory;

	@Test
	public void testCustomBuilder() {
		HttpClientBuilder builder = this.apacheHttpClientFactory.createBuilder();
		then(CustomHttpClientBuilderApplication.MyHttpClientBuilder.class
				.isInstance(builder)).isTrue();
	}

}

@Configuration
@EnableAutoConfiguration
class CustomHttpClientBuilderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

	@Configuration
	@AutoConfigureBefore(HttpClientConfiguration.class)
	static class MyConfig {

		@Bean
		public MyHttpClientBuilder builder() {
			return new MyHttpClientBuilder();
		}

	}

	static class MyHttpClientBuilder extends HttpClientBuilder {

	}

}
