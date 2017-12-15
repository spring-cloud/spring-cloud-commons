package org.springframework.cloud.commons.httpclient;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.config.Lookup;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Ryan Baxter
 * @author Michael Wirth
 */
public class DefaultApacheHttpClientConnectionManagerFactoryTests {
	@Test
	public void newConnectionManager() throws Exception {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(false, 2, 6);
		assertEquals(6, ((PoolingHttpClientConnectionManager) connectionManager)
				.getDefaultMaxPerRoute());
		assertEquals(2,
				((PoolingHttpClientConnectionManager) connectionManager).getMaxTotal());
		Object pool = getField(((PoolingHttpClientConnectionManager) connectionManager),
				"pool");
		assertEquals(new Long(-1), getField(pool, "timeToLive"));
		TimeUnit timeUnit = getField(pool, "tunit");
		assertEquals(TimeUnit.MILLISECONDS, timeUnit);
	}

	@Test
	public void newConnectionManagerWithTTL() throws Exception {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(false, 2, 6, 56l, TimeUnit.DAYS, null);
		assertEquals(6, ((PoolingHttpClientConnectionManager) connectionManager)
				.getDefaultMaxPerRoute());
		assertEquals(2,
				((PoolingHttpClientConnectionManager) connectionManager).getMaxTotal());
		Object pool = getField(((PoolingHttpClientConnectionManager) connectionManager),
				"pool");
		assertEquals(new Long(56), getField(pool, "timeToLive"));
		TimeUnit timeUnit = getField(pool, "tunit");
		assertEquals(TimeUnit.DAYS, timeUnit);
	}

	@Test
	public void newConnectionManagerWithSSL() throws Exception {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(false, 2, 6);

		Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(
				connectionManager);
		assertThat(socketFactoryRegistry.lookup("https"), is(notNullValue()));
		assertThat(getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers(),
				is(notNullValue()));
	}

	@Test
	public void newConnectionManagerWithDisabledSSLValidation() throws Exception {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(true, 2, 6);

		Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(
				connectionManager);
		assertThat(socketFactoryRegistry.lookup("https"), is(notNullValue()));
		assertThat(getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers(),
				is(nullValue()));
	}

	private Lookup<ConnectionSocketFactory> getConnectionSocketFactoryLookup(
			HttpClientConnectionManager connectionManager) {
		DefaultHttpClientConnectionOperator connectionOperator = getField(
				connectionManager, "connectionOperator");
		return getField(connectionOperator, "socketFactoryRegistry");
	}

	private X509TrustManager getX509TrustManager(
			Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
		ConnectionSocketFactory connectionSocketFactory = socketFactoryRegistry
				.lookup("https");
		SSLSocketFactory sslSocketFactory = getField(connectionSocketFactory,
				"socketfactory");
		SSLContextSpi sslContext = getField(sslSocketFactory, "context");
		return getField(sslContext, "trustManager");
	}

	@SuppressWarnings("unchecked")
	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}
}
