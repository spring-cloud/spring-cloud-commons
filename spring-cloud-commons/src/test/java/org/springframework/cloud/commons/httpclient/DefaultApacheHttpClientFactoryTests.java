package org.springframework.cloud.commons.httpclient;

import java.lang.reflect.Field;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactoryTests {
	@Test
	public void createClient() throws Exception {
		final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(100)
				.setConnectTimeout(200).setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = new DefaultApacheHttpClientFactory(HttpClientBuilder.create()).createBuilder().
				setConnectionManager(mock(HttpClientConnectionManager.class)).
				setDefaultRequestConfig(requestConfig).build();
		Assertions.assertThat(httpClient).isInstanceOf(Configurable.class);
		RequestConfig config = ((Configurable) httpClient).getConfig();
		assertEquals(100, config.getSocketTimeout());
		assertEquals(200, config.getConnectTimeout());
		assertEquals(CookieSpecs.IGNORE_COOKIES, config.getCookieSpec());
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}