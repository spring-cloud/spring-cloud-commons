package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientConnectionPoolFactoryTest {
	@Test
	public void create() throws Exception {
		DefaultOkHttpClientConnectionPoolFactory connectionPoolFactory = new DefaultOkHttpClientConnectionPoolFactory();
		ConnectionPool connectionPool = connectionPoolFactory.create(2,
				3, TimeUnit.MILLISECONDS);
		int idleConnections = getField(connectionPool, "maxIdleConnections");
		long keepAliveDuration = getField(connectionPool, "keepAliveDurationNs");
		assertEquals(2, idleConnections);
		assertEquals(TimeUnit.MILLISECONDS.toNanos(3), keepAliveDuration);
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}