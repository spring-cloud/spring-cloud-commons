/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.commons.httpclient;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Factory for creating a new {@link CloseableHttpClient}.
 * @author Ryan Baxter
 */
public interface ApacheHttpClientFactory {

	/**
	 * Creates a new {@link CloseableHttpClient}.
	 * @param requestConfig Configuration to be used for all requests by default
	 * @param connectionManager The {@link HttpClientConnectionManager} to use for the
	 * client
	 * @return A new {@link CloseableHttpClient}
	 */
	public CloseableHttpClient createClient(RequestConfig requestConfig,
			HttpClientConnectionManager connectionManager);
}
