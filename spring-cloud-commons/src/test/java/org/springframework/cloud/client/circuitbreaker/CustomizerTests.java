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

package org.springframework.cloud.client.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CustomizerTests {

	@Test
	public void testCustomizedOnlyOnce() {
		AtomicInteger counter = new AtomicInteger(0);
		final Customizer<AtomicInteger> customizer = Customizer
				.once(AtomicInteger::incrementAndGet, Object::hashCode);
		customizer.customize(counter);
		customizer.customize(counter);
		customizer.customize(counter);
		Assertions.assertThat(counter.get()).isEqualTo(1);

		AtomicInteger anotherCounter = new AtomicInteger(0);
		customizer.customize(anotherCounter);
		customizer.customize(anotherCounter);
		Assertions.assertThat(counter.get()).isEqualTo(1);
	}

}
