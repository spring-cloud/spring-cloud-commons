/*
 * Copyright 2012-2022 the original author or authors.
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

import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.apache.http.client.config.CookieSpecs;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
class DefaultApacheHttpClient5FactoryTests {

	@Test
	public void createClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(100)).setConnectTimeout(Timeout.ofMilliseconds(200))
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = new DefaultApacheHttpClient5Factory(HttpClientBuilder.create()).createBuilder()
				.setConnectionManager(mock(HttpClientConnectionManager.class)).setDefaultRequestConfig(requestConfig)
				.build();
		BDDAssertions.then(httpClient).isInstanceOf(Configurable.class);
		RequestConfig config = ((Configurable) httpClient).getConfig();
		then(config.getConnectionRequestTimeout()).isEqualTo(Timeout.ofMilliseconds(100));
		then(config.getConnectTimeout()).isEqualTo(Timeout.ofMilliseconds(200));
		then(config.getCookieSpec()).isEqualTo(CookieSpecs.IGNORE_COOKIES);
	}

}
