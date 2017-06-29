package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Creates new {@link OkHttpClient}s.
 * @author Ryan Baxter
 */
public interface OkHttpClientFactory {

	/**
	 * Creates a new {@link OkHttpClient}.
	 * @param disableSslValidation To disable SSL validation
	 * @param connectTimeout Connection timeout duration
	 * @param connectTimeoutUnit Connection timeout time unit
	 * @param followRedirects Whether to follow redirects
	 * @param readTimeout Read timeout duration
	 * @param readTimeoutUnit Read timeout time unit
	 * @param connectionPool The connection pool to use
	 * @param sslSocketFactory The socket factory to use, can be {@code null}
	 * @param x509TrustManager The trust manager to use, can be {@code null}
	 * @return A new {@link OkHttpClient}
	 */
	public OkHttpClient create(boolean disableSslValidation, long connectTimeout,
			TimeUnit connectTimeoutUnit, boolean followRedirects, long readTimeout,
			TimeUnit readTimeoutUnit, ConnectionPool connectionPool,
			SSLSocketFactory sslSocketFactory, X509TrustManager x509TrustManager);

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
