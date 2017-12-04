package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;

import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link OkHttpClientConnectionPoolFactory}.
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientConnectionPoolFactory implements OkHttpClientConnectionPoolFactory {

	@Override
	public ConnectionPool create(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
		return new ConnectionPool(maxIdleConnections, keepAliveDuration, timeUnit);
	}
}
