/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Customizes the parameterized class.
 *
 * @author Ryan Baxter
 * @author Toshiaki Maki
 */
public interface Customizer<TOCUSTOMIZE> {

	void customize(TOCUSTOMIZE tocustomize);

	/**
	 * Create a wrapped customizer that guarantees that the {@link #customize(Object)}
	 * method of the delegated <code>customizer</code> is called at most once per target.
	 * @param customizer a customizer to be delegated
	 * @param keyMapper a mapping function to produce the identifier of the target
	 * @param <T> the type of the target to customize
	 * @param <K> the type of the identifier of the target
	 * @return a wrapped customizer
	 */
	static <T, K> Customizer<T> once(Customizer<T> customizer,
			Function<? super T, ? extends K> keyMapper) {
		final Set<K> customized = Collections.newSetFromMap(new ConcurrentHashMap<>());
		return t -> {
			final K key = keyMapper.apply(t);
			if (!customized.contains(key)) {
				customized.add(key);
				customizer.customize(t);
			}
		};
	}

}
