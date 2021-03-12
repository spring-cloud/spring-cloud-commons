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

package org.springframework.cloud.bootstrap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Spencer Gibb
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestHigherPriorityBootstrapConfiguration {

	public static final AtomicInteger count = new AtomicInteger();
	static final AtomicReference<Class<?>> firstToBeCreated = new AtomicReference<>();

	public TestHigherPriorityBootstrapConfiguration() {
		count.incrementAndGet();
		firstToBeCreated.compareAndSet(null, TestHigherPriorityBootstrapConfiguration.class);
	}

}
