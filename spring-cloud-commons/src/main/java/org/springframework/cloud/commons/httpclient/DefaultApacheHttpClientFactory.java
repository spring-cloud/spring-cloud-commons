package org.springframework.cloud.commons.httpclient;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Default implementation of {@link ApacheHttpClientFactory}.
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactory implements ApacheHttpClientFactory {

	/**
	 * A default {@link HttpClientBuilder}.  The {@link HttpClientBuilder} returned will
	 * have content compression disabled, cookie management disabled, and use system properties.
	 */
	@Override
	public HttpClientBuilder createBuilder() {
		return HttpClientBuilder.create().disableContentCompression()
				.disableCookieManagement().useSystemProperties();
	}
}
