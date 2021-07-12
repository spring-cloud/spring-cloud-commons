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

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
@SpringBootTest(classes = CustomOkHttpClientBuilderApplication.class)
public class CustomOkHttpClientBuilderConfigurationTests {

	@Autowired
	private OkHttpClientFactory okHttpClientFactory;

	@Test
	public void testCustomBuilder() {
		OkHttpClient.Builder builder = this.okHttpClientFactory.createBuilder(false);
		Integer timeout = getField(builder, "connectTimeout");
		then(timeout.intValue()).isEqualTo(1);
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class CustomOkHttpClientBuilderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfig {

		@Bean
		public OkHttpClient.Builder builder() {
			return new OkHttpClient.Builder().connectTimeout(1, TimeUnit.MILLISECONDS);
		}

	}

}
