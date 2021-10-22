/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 * @since 3.1.0
 */
@ConfigurationProperties("spring.cloud.loadbalancer.clients")
public class LoadBalancerClientsProperties implements Map<String, LoadBalancerProperties> {

	Map<String, LoadBalancerProperties> clients = new HashMap<>();

	@Override
	public int size() {
		return clients.size();
	}

	@Override
	public boolean isEmpty() {
		return clients.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return clients.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return clients.containsValue(value);
	}

	@Override
	public LoadBalancerProperties get(Object key) {
		return clients.get(key);
	}

	@Override
	public LoadBalancerProperties put(String key, LoadBalancerProperties value) {
		return clients.put(key, value);
	}

	@Override
	public LoadBalancerProperties remove(Object key) {
		return clients.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends LoadBalancerProperties> m) {
		clients.putAll(m);
	}

	@Override
	public void clear() {
		clients.clear();
	}

	@Override
	public Set<String> keySet() {
		return clients.keySet();
	}

	@Override
	public Collection<LoadBalancerProperties> values() {
		return clients.values();
	}

	@Override
	public Set<Entry<String, LoadBalancerProperties>> entrySet() {
		return clients.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return clients.equals(o);
	}

	@Override
	public int hashCode() {
		return clients.hashCode();
	}

	@Override
	public LoadBalancerProperties getOrDefault(Object key, LoadBalancerProperties defaultValue) {
		return clients.getOrDefault(key, defaultValue);
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super LoadBalancerProperties> action) {
		clients.forEach(action);
	}

	@Override
	public void replaceAll(
			BiFunction<? super String, ? super LoadBalancerProperties, ? extends LoadBalancerProperties> function) {
		clients.replaceAll(function);
	}

	@Override
	public LoadBalancerProperties putIfAbsent(String key, LoadBalancerProperties value) {
		return clients.putIfAbsent(key, value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return clients.remove(key, value);
	}

	@Override
	public boolean replace(String key, LoadBalancerProperties oldValue, LoadBalancerProperties newValue) {
		return clients.replace(key, oldValue, newValue);
	}

	@Override
	public LoadBalancerProperties replace(String key, LoadBalancerProperties value) {
		return clients.replace(key, value);
	}

	@Override
	public LoadBalancerProperties computeIfAbsent(String key,
			Function<? super String, ? extends LoadBalancerProperties> mappingFunction) {
		return clients.computeIfAbsent(key, mappingFunction);
	}

	@Override
	public LoadBalancerProperties computeIfPresent(String key,
			BiFunction<? super String, ? super LoadBalancerProperties, ? extends LoadBalancerProperties> remappingFunction) {
		return clients.computeIfPresent(key, remappingFunction);
	}

	@Override
	public LoadBalancerProperties compute(String key,
			BiFunction<? super String, ? super LoadBalancerProperties, ? extends LoadBalancerProperties> remappingFunction) {
		return clients.compute(key, remappingFunction);
	}

	@Override
	public LoadBalancerProperties merge(String key, LoadBalancerProperties value,
			BiFunction<? super LoadBalancerProperties, ? super LoadBalancerProperties, ? extends LoadBalancerProperties> remappingFunction) {
		return clients.merge(key, value, remappingFunction);
	}

}
