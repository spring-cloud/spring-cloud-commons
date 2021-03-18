/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 * @author Michael Wirth
 */
public class DefaultApacheHttpClientConnectionManagerFactoryTests {

	@Test
	public void newConnectionManager() {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(false, 2, 6);
		then(((PoolingHttpClientConnectionManager) connectionManager).getDefaultMaxPerRoute()).isEqualTo(6);
		then(((PoolingHttpClientConnectionManager) connectionManager).getMaxTotal()).isEqualTo(2);
		Object pool = getField((connectionManager), "pool");
		then((Long) getField(pool, "timeToLive")).isEqualTo(new Long(-1));
		TimeUnit timeUnit = getField(pool, "timeUnit");
		then(timeUnit).isEqualTo(TimeUnit.MILLISECONDS);
	}

	@Test
	public void newConnectionManagerWithTTL() {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(false, 2, 6, 56L, TimeUnit.DAYS, null);
		then(((PoolingHttpClientConnectionManager) connectionManager).getDefaultMaxPerRoute()).isEqualTo(6);
		then(((PoolingHttpClientConnectionManager) connectionManager).getMaxTotal()).isEqualTo(2);
		Object pool = getField((connectionManager), "pool");
		then((Long) getField(pool, "timeToLive")).isEqualTo(new Long(56));
		TimeUnit timeUnit = getField(pool, "timeUnit");
		then(timeUnit).isEqualTo(TimeUnit.DAYS);
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void newConnectionManagerWithSSL() {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(false, 2, 6);

		Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(connectionManager);
		then(socketFactoryRegistry.lookup("https")).isNotNull();
		then(getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers()).isNotNull();
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void newConnectionManagerWithDisabledSSLValidation() {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClientConnectionManagerFactory()
				.newConnectionManager(true, 2, 6);

		Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(connectionManager);
		then(socketFactoryRegistry.lookup("https")).isNotNull();
		then(getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers()).isNull();
	}

	private Lookup<ConnectionSocketFactory> getConnectionSocketFactoryLookup(
			HttpClientConnectionManager connectionManager) {
		DefaultHttpClientConnectionOperator connectionOperator = getField(connectionManager, "connectionOperator");
		return getField(connectionOperator, "socketFactoryRegistry");
	}

	private X509TrustManager getX509TrustManager(Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
		ConnectionSocketFactory connectionSocketFactory = socketFactoryRegistry.lookup("https");
		SSLSocketFactory sslSocketFactory = getField(connectionSocketFactory, "socketfactory");
		SSLContextSpi sslContext = getField(sslSocketFactory, "context");
		return getField(sslContext, "trustManager");
	}

	@SuppressWarnings("unchecked")
	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalArgumentException("Can not find field " + name + " in " + target.getClass());
		}
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}
