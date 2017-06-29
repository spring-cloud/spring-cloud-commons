package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of {@link OkHttpClientFactory}.
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientFactory implements OkHttpClientFactory {

	private static final Log LOG = LogFactory.getLog(DefaultOkHttpClientFactory.class);

	@Override
	public OkHttpClient create(boolean disableSslValidation, long connectTimeout,
			TimeUnit connectTimeoutUnit, boolean followRedirects, long readTimeout,
			TimeUnit readTimeoutUnit, ConnectionPool connectionPool,
			SSLSocketFactory sslSocketFactory, X509TrustManager x509TrustManager) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder()
				.connectTimeout(connectTimeout, connectTimeoutUnit)
				.followRedirects(followRedirects)
				.readTimeout(readTimeout, readTimeoutUnit).connectionPool(connectionPool)
				.connectionPool(connectionPool);
		if (disableSslValidation) {
			try {
				X509TrustManager disabledTrustManager = new DisableValidationTrustManager();
				TrustManager[] trustManagers = new TrustManager[1];
				trustManagers[0] = disabledTrustManager;
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, trustManagers, new java.security.SecureRandom());
				SSLSocketFactory disabledSSLSocketFactory = sslContext.getSocketFactory();
				builder.sslSocketFactory(disabledSSLSocketFactory, disabledTrustManager);
				builder.hostnameVerifier(new TrustAllHostnames());
			}
			catch (NoSuchAlgorithmException e) {
				LOG.warn("Error setting SSLSocketFactory in OKHttpClient", e);
			}
			catch (KeyManagementException e) {
				LOG.warn("Error setting SSLSocketFactory in OKHttpClient", e);
			}
		}
		if (sslSocketFactory != null && x509TrustManager != null) {
			builder.sslSocketFactory(sslSocketFactory, x509TrustManager);
		}
		return builder.build();
	}
}
