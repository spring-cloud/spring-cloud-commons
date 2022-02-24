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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientFactoryTest {

	@Test
	public void create() throws Exception {
		DefaultOkHttpClientFactory okHttpClientFactory = new DefaultOkHttpClientFactory(new OkHttpClient.Builder());
		DefaultOkHttpClientConnectionPoolFactory poolFactory = new DefaultOkHttpClientConnectionPoolFactory();
		ConnectionPool pool = poolFactory.create(4, 5, TimeUnit.DAYS);
		OkHttpClient httpClient = okHttpClientFactory.createBuilder(true).connectTimeout(2, TimeUnit.MILLISECONDS)
				.readTimeout(3, TimeUnit.HOURS).followRedirects(true).connectionPool(pool).build();
		then(httpClient.connectTimeoutMillis()).isEqualTo(2);
		then(httpClient.readTimeoutMillis()).isEqualTo(TimeUnit.HOURS.toMillis(3));
		then(httpClient.followRedirects()).isTrue();
		then(httpClient.connectionPool()).isEqualTo(pool);
		then(OkHttpClientFactory.TrustAllHostnames.class.isInstance(httpClient.hostnameVerifier())).isTrue();
	}

}
