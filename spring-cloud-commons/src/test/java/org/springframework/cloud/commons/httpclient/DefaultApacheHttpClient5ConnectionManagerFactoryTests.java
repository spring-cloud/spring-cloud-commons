/*
 * Copyright 2012-2022 the original author or authors.
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

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
class DefaultApacheHttpClient5ConnectionManagerFactoryTests {

	@Test
	public void newConnectionManager() {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClient5ConnectionManagerFactory()
				.newConnectionManager(false, 2, 6);
		then(((PoolingHttpClientConnectionManager) connectionManager).getDefaultMaxPerRoute()).isEqualTo(6);
		then(((PoolingHttpClientConnectionManager) connectionManager).getMaxTotal()).isEqualTo(2);
		Object pool = getField((connectionManager), "pool");
		then((TimeValue) getField(pool, "timeToLive")).isEqualTo(TimeValue.ofMilliseconds(-1));
	}

	@Test
	public void newConnectionManagerWithTTL() {
		HttpClientConnectionManager connectionManager = new DefaultApacheHttpClient5ConnectionManagerFactory()
				.newConnectionManager(false, 2, 6, 56L, TimeUnit.DAYS, null);
		then(((PoolingHttpClientConnectionManager) connectionManager).getDefaultMaxPerRoute()).isEqualTo(6);
		then(((PoolingHttpClientConnectionManager) connectionManager).getMaxTotal()).isEqualTo(2);
		Object pool = getField((connectionManager), "pool");
		then((TimeValue) getField(pool, "timeToLive")).isEqualTo(TimeValue.ofDays(56));
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
