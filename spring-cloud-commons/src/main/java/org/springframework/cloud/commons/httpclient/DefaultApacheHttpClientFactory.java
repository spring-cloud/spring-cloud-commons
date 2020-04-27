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

import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Default implementation of {@link ApacheHttpClientFactory}.
 *
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactory implements ApacheHttpClientFactory {

	private HttpClientBuilder builder;

	public DefaultApacheHttpClientFactory(HttpClientBuilder builder) {
		this.builder = builder;
	}

	/**
	 * A default {@link HttpClientBuilder}. The {@link HttpClientBuilder} returned will
	 * have content compression disabled, have cookie management disabled, and use system
	 * properties.
	 */
	@Override
	public HttpClientBuilder createBuilder() {
		return this.builder.disableContentCompression().disableCookieManagement()
				.useSystemProperties();
	}

}
