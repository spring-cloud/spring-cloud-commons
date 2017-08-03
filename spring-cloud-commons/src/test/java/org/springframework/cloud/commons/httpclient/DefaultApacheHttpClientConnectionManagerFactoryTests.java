package org.springframework.cloud.commons.httpclient;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * @author Ryan Baxter
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
		assertEquals(-1l, getField(pool, "timeToLive"));
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
		assertEquals(56l, getField(pool, "timeToLive"));
		TimeUnit timeUnit = getField(pool, "tunit");
		assertEquals(TimeUnit.DAYS, timeUnit);
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}
}