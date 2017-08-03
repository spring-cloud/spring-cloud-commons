package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;

import java.util.concurrent.TimeUnit;

/**
 * Creates {@link ConnectionPool}s for {@link okhttp3.OkHttpClient}s
 * @author Ryan Baxter
 */
public interface OkHttpClientConnectionPoolFactory {

	/**
	 * Creates a new {@link ConnectionPool}.
	 * @param maxIdleConnections number of max idle connections to allow
	 * @param keepAliveDuration amount of time to keep connections alive
	 * @param timeUnit the time unit for the keep alive duration
	 * @return A new {@link ConnectionPool}
	 */
	public ConnectionPool create(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit);
}
