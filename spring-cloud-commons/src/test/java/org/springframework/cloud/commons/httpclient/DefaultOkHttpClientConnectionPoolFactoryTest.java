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

import okhttp3.ConnectionPool;
import okhttp3.internal.connection.RealConnectionPool;
import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
public class DefaultOkHttpClientConnectionPoolFactoryTest {

	@Test
	public void create() {
		DefaultOkHttpClientConnectionPoolFactory connectionPoolFactory = new DefaultOkHttpClientConnectionPoolFactory();
		ConnectionPool connectionPool = connectionPoolFactory.create(2, 3,
				TimeUnit.MILLISECONDS);
		RealConnectionPool delegate = getField(connectionPool, "delegate");
		int idleConnections = getField(delegate, "maxIdleConnections");
		long keepAliveDuration = getField(delegate, "keepAliveDurationNs");
		then(idleConnections).isEqualTo(2);
		then(keepAliveDuration).isEqualTo(TimeUnit.MILLISECONDS.toNanos(3));
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalArgumentException(
					"Can not find field " + name + " in " + target.getClass());
		}
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}
