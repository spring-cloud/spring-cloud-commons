package org.springframework.cloud.commons.httpclient;

import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Default implementation of {@link ApacheHttpClientFactory}.
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactory implements ApacheHttpClientFactory {

	private HttpClientBuilder builder;

	public DefaultApacheHttpClientFactory(HttpClientBuilder builder) {
		this.builder = builder;
	}

	/**
	 * A default {@link HttpClientBuilder}. The {@link HttpClientBuilder} returned will
	 * have content compression disabled, cookie management disabled, and use system properties.
	 */
	@Override
	public HttpClientBuilder createBuilder() {
		return this.builder.disableContentCompression()
				.disableCookieManagement().useSystemProperties();
	}
}
