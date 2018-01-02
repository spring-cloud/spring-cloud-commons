package org.springframework.cloud.commons.httpclient;

import okhttp3.OkHttpClient;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * Creates new {@link OkHttpClient}s.
 * @author Ryan Baxter
 */
public interface OkHttpClientFactory {

	/**
	 * Creates a {@link OkHttpClient.Builder} used to build an {@link OkHttpClient}.
	 * @param disableSslValidation Disables SSL validation
	 * @return A new {@link OkHttpClient.Builder}
	 */
	public OkHttpClient.Builder createBuilder(boolean disableSslValidation);

	/**
	 * A {@link X509TrustManager} that does not validate SSL certificates.
	 */
	public static class DisableValidationTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}

	/**
	 * A {@link HostnameVerifier} that does not validate any hostnames.
	 */
	public static class TrustAllHostnames implements HostnameVerifier {

		@Override
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}
	}
}
