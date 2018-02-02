package org.springframework.cloud.commons.httpclient;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientFactoryTest {
	@Test
	public void create() throws Exception {
		DefaultOkHttpClientFactory okHttpClientFactory = new DefaultOkHttpClientFactory(new OkHttpClient.Builder());
		DefaultOkHttpClientConnectionPoolFactory poolFactory = new DefaultOkHttpClientConnectionPoolFactory();
		ConnectionPool pool = poolFactory.create(4, 5, TimeUnit.DAYS);
		OkHttpClient httpClient = okHttpClientFactory.createBuilder(true).
				connectTimeout(2, TimeUnit.MILLISECONDS).
				readTimeout(3, TimeUnit.HOURS).
				followRedirects(true).
				connectionPool(pool).build();
		int connectTimeout = getField(httpClient, "connectTimeout");
		assertEquals(2, connectTimeout);
		int readTimeout = getField(httpClient, "readTimeout");
		assertEquals(TimeUnit.HOURS.toMillis(3), readTimeout);
		boolean followRedirects = getField(httpClient, "followRedirects");
		assertTrue(followRedirects);
		ConnectionPool poolFromClient = getField(httpClient, "connectionPool");
		assertEquals(pool, poolFromClient);
		HostnameVerifier hostnameVerifier = getField(httpClient, "hostnameVerifier");
		assertTrue(OkHttpClientFactory.TrustAllHostnames.class.isInstance(hostnameVerifier));
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}
}