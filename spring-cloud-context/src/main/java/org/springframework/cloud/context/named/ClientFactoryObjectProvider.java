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

package org.springframework.cloud.context.named;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;

/**
 * Special ObjectProvider that allows the actual ObjectProvider to be resolved later
 * because of the creation of the named child context.
 *
 * @param <T> - type of the provided object
 */
class ClientFactoryObjectProvider<T> implements ObjectProvider<T> {

	private final NamedContextFactory<?> clientFactory;

	private final String name;

	private final Class<T> type;

	private ObjectProvider<T> provider;

	ClientFactoryObjectProvider(NamedContextFactory<?> clientFactory, String name,
			Class<T> type) {
		this.clientFactory = clientFactory;
		this.name = name;
		this.type = type;
	}

	@Override
	public T getObject(Object... args) throws BeansException {
		return delegate().getObject(args);
	}

	@Override
	@Nullable
	public T getIfAvailable() throws BeansException {
		return delegate().getIfAvailable();
	}

	@Override
	public T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		return delegate().getIfAvailable(defaultSupplier);
	}

	@Override
	public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		delegate().ifAvailable(dependencyConsumer);
	}

	@Override
	@Nullable
	public T getIfUnique() throws BeansException {
		return delegate().getIfUnique();
	}

	@Override
	public T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		return delegate().getIfUnique(defaultSupplier);
	}

	@Override
	public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		delegate().ifUnique(dependencyConsumer);
	}

	@Override
	public Iterator<T> iterator() {
		return delegate().iterator();
	}

	@Override
	public Stream<T> stream() {
		return delegate().stream();
	}

	@Override
	public T getObject() throws BeansException {
		return delegate().getObject();
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		delegate().forEach(action);
	}

	@Override
	public Spliterator<T> spliterator() {
		return delegate().spliterator();
	}

	private ObjectProvider<T> delegate() {
		if (this.provider == null) {
			this.provider = this.clientFactory.getProvider(this.name, this.type);
		}
		return this.provider;
	}

}
