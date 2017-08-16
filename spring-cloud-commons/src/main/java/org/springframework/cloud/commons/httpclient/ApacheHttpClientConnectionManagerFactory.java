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

import java.util.concurrent.TimeUnit;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;

/**
 * Interface for creating an {@link HttpClientConnectionManager}.
 * @author Ryan Baxter
 */
public interface ApacheHttpClientConnectionManagerFactory {
	public static final String HTTP_SCHEME = "http";
	public static final String HTTPS_SCHEME = "https";

	/**
	 * Creates a new {@link HttpClientConnectionManager}.
	 * @param disableSslValidation True to disable SSL validation, false otherwise
	 * @param maxTotalConnections The total number of connections
	 * @param maxConnectionsPerRoute The total number of connections per route
	 * @param timeToLive The time a connection is allowed to exist
	 * @param timeUnit The time unit for the time to live value
	 * @param registryBuilder The {@link RegistryBuilder} to use in the connection manager
	 * @return A new {@link HttpClientConnectionManager}
	 */
	public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation,
			int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive,
			TimeUnit timeUnit, RegistryBuilder registryBuilder);
}
