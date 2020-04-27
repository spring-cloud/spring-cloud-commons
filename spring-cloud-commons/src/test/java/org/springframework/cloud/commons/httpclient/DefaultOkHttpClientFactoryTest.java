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

import javax.net.ssl.HostnameVerifier;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientFactoryTest {

	@Test
	public void create() throws Exception {
		DefaultOkHttpClientFactory okHttpClientFactory = new DefaultOkHttpClientFactory(
				new OkHttpClient.Builder());
		DefaultOkHttpClientConnectionPoolFactory poolFactory = new DefaultOkHttpClientConnectionPoolFactory();
		ConnectionPool pool = poolFactory.create(4, 5, TimeUnit.DAYS);
		OkHttpClient httpClient = okHttpClientFactory.createBuilder(true)
				.connectTimeout(2, TimeUnit.MILLISECONDS).readTimeout(3, TimeUnit.HOURS)
				.followRedirects(true).connectionPool(pool).build();
		int connectTimeout = getField(httpClient, "connectTimeout");
		then(connectTimeout).isEqualTo(2);
		int readTimeout = getField(httpClient, "readTimeout");
		then(readTimeout).isEqualTo(TimeUnit.HOURS.toMillis(3));
		boolean followRedirects = getField(httpClient, "followRedirects");
		then(followRedirects).isTrue();
		ConnectionPool poolFromClient = getField(httpClient, "connectionPool");
		then(poolFromClient).isEqualTo(pool);
		HostnameVerifier hostnameVerifier = getField(httpClient, "hostnameVerifier");
		then(OkHttpClientFactory.TrustAllHostnames.class.isInstance(hostnameVerifier))
				.isTrue();
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}
