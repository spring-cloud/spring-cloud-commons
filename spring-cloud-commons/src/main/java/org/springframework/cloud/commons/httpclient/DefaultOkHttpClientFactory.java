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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of {@link OkHttpClientFactory}.
 *
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientFactory implements OkHttpClientFactory {

	private static final Log LOG = LogFactory.getLog(DefaultOkHttpClientFactory.class);

	private OkHttpClient.Builder builder;

	public DefaultOkHttpClientFactory(OkHttpClient.Builder builder) {
		this.builder = builder;
	}

	@Override
	public OkHttpClient.Builder createBuilder(boolean disableSslValidation) {
		if (disableSslValidation) {
			try {
				X509TrustManager disabledTrustManager = new DisableValidationTrustManager();
				TrustManager[] trustManagers = new TrustManager[1];
				trustManagers[0] = disabledTrustManager;
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, trustManagers, new java.security.SecureRandom());
				SSLSocketFactory disabledSSLSocketFactory = sslContext.getSocketFactory();
				this.builder.sslSocketFactory(disabledSSLSocketFactory,
						disabledTrustManager);
				this.builder.hostnameVerifier(new TrustAllHostnames());
			}
			catch (NoSuchAlgorithmException e) {
				LOG.warn("Error setting SSLSocketFactory in OKHttpClient", e);
			}
			catch (KeyManagementException e) {
				LOG.warn("Error setting SSLSocketFactory in OKHttpClient", e);
			}
		}
		return this.builder;
	}

}
