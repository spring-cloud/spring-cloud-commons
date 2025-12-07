/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerWebClientBuilderBeanPostProcessor;

/**
 * Wrapper for {@link ObjectProvider}. Added to use for a workaround in
 * {@link LoadBalancerWebClientBuilderBeanPostProcessor}.
 *
 * @param <T> type of the object to fetch
 * @author Spencer Gibb
 * @deprecated for removal in 4.0
 */
@Deprecated(forRemoval = true)
public class SimpleObjectProvider<T> implements ObjectProvider<T> {

	private final T object;

	public SimpleObjectProvider(T object) {
		this.object = object;
	}

	@Override
	public T getObject(@Nullable Object... args) throws BeansException {
		return this.object;
	}

	@Override
	public @Nullable T getIfAvailable() throws BeansException {
		return this.object;
	}

	@Override
	public @Nullable T getIfUnique() throws BeansException {
		return this.object;
	}

	@Override
	public T getObject() throws BeansException {
		return this.object;
	}

}
