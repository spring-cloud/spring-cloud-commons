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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.TimeValue;

/**
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClient5ConnectionManagerFactory
		implements ApacheHttpClientConnectionManagerFactory<HttpClientConnectionManager, RegistryBuilder> {

	private static final Log LOG = LogFactory.getLog(DefaultApacheHttpClient5ConnectionManagerFactory.class);

	public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections,
			int maxConnectionsPerRoute) {
		return newConnectionManager(disableSslValidation, maxTotalConnections, maxConnectionsPerRoute, -1,
				TimeUnit.MILLISECONDS, null);
	}

	@Override
	public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections,
			int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registryBuilder) {
		if (registryBuilder == null) {
			registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create().register(HTTP_SCHEME,
					PlainConnectionSocketFactory.INSTANCE);
		}
		if (disableSslValidation) {
			try {
				final SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new TrustManager[] { new DisabledValidationTrustManager() }, new SecureRandom());
				registryBuilder.register(HTTPS_SCHEME,
						new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE));
			}
			catch (NoSuchAlgorithmException | KeyManagementException e) {
				LOG.warn("Error creating SSLContext", e);
			}
		}
		else {
			registryBuilder.register("https", SSLConnectionSocketFactory.getSocketFactory());
		}
		final Registry<ConnectionSocketFactory> registry = registryBuilder.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry, null,
				null, TimeValue.of(timeToLive, timeUnit));
		connectionManager.setMaxTotal(maxTotalConnections);
		connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

		return connectionManager;
	}

	static class DisabledValidationTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

	}

}
