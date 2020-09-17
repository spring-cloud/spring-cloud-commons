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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Creates new {@link OkHttpClient}s.
 *
 * @author Ryan Baxter
 */
public interface OkHttpClientFactory {

	/**
	 * Creates a {@link OkHttpClient.Builder} used to build an {@link OkHttpClient}.
	 * @param disableSslValidation Disables SSL validation
	 * @return A new {@link OkHttpClient.Builder}
	 */
	OkHttpClient.Builder createBuilder(boolean disableSslValidation);

	/**
	 * A {@link X509TrustManager} that does not validate SSL certificates.
	 */
	class DisableValidationTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

	}

	/**
	 * A {@link HostnameVerifier} that does not validate any hostnames.
	 */
	class TrustAllHostnames implements HostnameVerifier {

		@Override
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}

	}

}
