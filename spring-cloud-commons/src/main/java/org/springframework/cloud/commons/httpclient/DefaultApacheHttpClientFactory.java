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

	public CloseableHttpClient createClient(RequestConfig requestConfig, HttpClientConnectionManager connectionManager) {
		return HttpClientBuilder.create().disableContentCompression()
				.setDefaultRequestConfig(requestConfig)
				.setConnectionManager(connectionManager)
				.disableCookieManagement()
				.useSystemProperties()
				.build();
	}
}
