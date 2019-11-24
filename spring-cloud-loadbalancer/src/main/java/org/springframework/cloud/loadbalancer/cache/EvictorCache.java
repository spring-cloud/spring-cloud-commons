/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.loadbalancer.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

import javax.validation.constraints.Null;

import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.scheduler.DelayedTaskEvictionScheduler;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Olga Maciaszek-Sharma
 */
public class EvictorCache extends AbstractValueAdaptingCache {

	private final String name;

	private final ConcurrentMapWithTimedEviction<Object, Object> cache;

	private final long evictMs;

	/**
	 * Create a new EvictorCache with the specified name and the given internal
	 * {@link ConcurrentMapWithTimedEviction} to use.
	 * @param name the name of the cache
	 * @param evictMs default time to Evict for the underlyin
	 * {@link ConcurrentMapWithTimedEviction}
	 * @param cache the ConcurrentMap to use as an internal store
	 * @param allowNullValues whether to allow {@code null} values (adapting them to an
	 * internal null holder value)
	 */
	public EvictorCache(String name, ConcurrentMapWithTimedEviction<Object, Object> cache,
			long evictMs, boolean allowNullValues) {
		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache;
		this.evictMs = evictMs;
	}

	/**
	 * Create a new EvictorCache with the specified name.
	 * @param name the name of the cache
	 */
	public EvictorCache(String name) {
		this(name, new ConcurrentHashMapWithTimedEviction<>(256,
				new DelayedTaskEvictionScheduler<>()), 0, true);
	}

	/**
	 * Create a new EvictorCache with the specified name.
	 * @param name the name of the cache
	 * @param evictMs default time to Evict for the underlyin
	 * {@link ConcurrentMapWithTimedEviction}
	 * @param allowNullValues whether to accept and convert {@code null} values for this
	 * cache
	 */
	public EvictorCache(String name, long evictMs, boolean allowNullValues) {
		this(name, new ConcurrentHashMapWithTimedEviction<>(256,
				new DelayedTaskEvictionScheduler<>()), evictMs, allowNullValues);
	}

	/**
	 * Create a new EvictorCache with the specified name.
	 * @param name the name of the cache
	 * @param allowNullValues whether to accept and convert {@code null} values for this
	 * cache
	 */
	public EvictorCache(String name, boolean allowNullValues) {
		this(name, new ConcurrentHashMapWithTimedEviction<>(256,
				new DelayedTaskEvictionScheduler<>()), 0, allowNullValues);
	}

	@Override
	@Null
	protected Object lookup(Object key) {
		return cache.get(key);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ConcurrentMap<Object, Object> getNativeCache() {
		return cache;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T) fromStoreValue(cache.computeIfAbsent(key, k -> {
			try {
				return toStoreValue(valueLoader.call());
			}
			catch (Throwable ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
		}));
	}

	public void put(Object key, @Nullable Object value, long evictMs) {
		cache.put(key, toStoreValue(value), evictMs);
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		Object existing = cache.putIfAbsent(key, toStoreValue(value), evictMs);
		return toValueWrapper(existing);
	}

	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value, long evictMs) {
		Object existing = cache.putIfAbsent(key, toStoreValue(value), evictMs);
		return toValueWrapper(existing);
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		cache.put(key, toStoreValue(value), evictMs);
	}

	@Override
	public void evict(Object key) {
		cache.remove(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return (cache.remove(key) != null);
	}

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public boolean invalidate() {
		boolean notEmpty = !cache.isEmpty();
		cache.clear();
		return notEmpty;
	}

}
