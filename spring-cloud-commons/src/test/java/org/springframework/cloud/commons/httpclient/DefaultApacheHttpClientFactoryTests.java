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

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactoryTests {

	@Test
	public void createClient() throws Exception {
		final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(100).setConnectTimeout(200)
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = new DefaultApacheHttpClientFactory(HttpClientBuilder.create()).createBuilder()
				.setConnectionManager(mock(HttpClientConnectionManager.class)).setDefaultRequestConfig(requestConfig)
				.build();
		BDDAssertions.then(httpClient).isInstanceOf(Configurable.class);
		RequestConfig config = ((Configurable) httpClient).getConfig();
		then(config.getSocketTimeout()).isEqualTo(100);
		then(config.getConnectTimeout()).isEqualTo(200);
		then(config.getCookieSpec()).isEqualTo(CookieSpecs.IGNORE_COOKIES);
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}
