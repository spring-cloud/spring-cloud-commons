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

package org.springframework.cloud.loadbalancer.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;

/**
 * Redis-backed cache for Spring Cloud LoadBalancer service instances.
 *
 */
final class RedisLoadBalancerCache extends AbstractValueAdaptingCache {

	private static final Log log = LogFactory.getLog(RedisLoadBalancerCache.class);

	private static final String CACHE_KEY_PREFIX = "spring-cloud-loadbalancer:";

	private static final String CACHE_KEY_SEPARATOR = ":";

	private static final int SERIALIZATION_VERSION = 1;

	private static final int MAX_METADATA_ENTRIES = 10_000;

	private static final int MAX_SERVICE_INSTANCES = 10_000;

	private final String name;

	private final Duration ttl;

	private final RedisTemplate<String, byte[]> redisTemplate;

	RedisLoadBalancerCache(String name, RedisConnectionFactory redisConnectionFactory, Duration ttl) {
		super(false);
		Assert.hasText(name, "Cache name must not be empty");
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		this.name = name;
		this.ttl = ttl;

		this.redisTemplate = new RedisTemplate<>();
		this.redisTemplate.setConnectionFactory(redisConnectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(RedisSerializer.byteArray());
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getNativeCache() {
		return this.redisTemplate;
	}

	@Override
	protected Object lookup(Object key) {
		try {
			byte[] value = this.redisTemplate.opsForValue().get(createRedisKey(key));
			return value == null ? null : deserialize(value);
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to retrieve value from Redis LoadBalancer cache. " + "Treating it as a cache miss.",
						ex);
			}
			return null;
		}
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		Assert.notNull(valueLoader, "ValueLoader must not be null");

		ValueWrapper wrapper = get(key);
		if (wrapper != null) {
			@SuppressWarnings("unchecked")
			T value = (T) wrapper.get();
			return value;
		}

		try {
			T value = valueLoader.call();
			put(key, value);
			return value;
		}
		catch (Exception ex) {
			throw new ValueRetrievalException(key, valueLoader, ex);
		}
	}

	@Override
	public void put(Object key, Object value) {
		Assert.isInstanceOf(List.class, value, "LoadBalancer cache values must be lists of ServiceInstance");

		byte[] serializedValue = serialize((List<?>) value);

		try {
			if (hasTtl()) {
				this.redisTemplate.opsForValue().set(createRedisKey(key), serializedValue, this.ttl);
			}
			else {
				this.redisTemplate.opsForValue().set(createRedisKey(key), serializedValue);
			}
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to write value to Redis LoadBalancer cache. Continuing without caching.", ex);
			}
		}
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		Assert.isInstanceOf(List.class, value, "LoadBalancer cache values must be lists of ServiceInstance");

		byte[] serializedValue = serialize((List<?>) value);

		try {
			Boolean inserted;
			if (hasTtl()) {
				inserted = this.redisTemplate.opsForValue().setIfAbsent(createRedisKey(key), serializedValue, this.ttl);
			}
			else {
				inserted = this.redisTemplate.opsForValue().setIfAbsent(createRedisKey(key), serializedValue);
			}

			return Boolean.TRUE.equals(inserted) ? null : get(key);
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to put value in Redis LoadBalancer cache. Continuing without caching.", ex);
			}
			return null;
		}
	}

	@Override
	public void evict(Object key) {
		this.redisTemplate.delete(createRedisKey(key));
	}

	@Override
	public void clear() {
		ScanOptions scanOptions = ScanOptions.scanOptions().match(createRedisKey("*")).count(1000).build();

		this.redisTemplate.execute((RedisCallback<Void>) connection -> {
			try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
				while (cursor.hasNext()) {
					connection.keyCommands().del(cursor.next());
				}
			}
			return null;
		});
	}

	private boolean hasTtl() {
		return this.ttl != null && !this.ttl.isZero() && !this.ttl.isNegative();
	}

	private String createRedisKey(Object key) {
		Assert.notNull(key, "Cache key must not be null");
		return CACHE_KEY_PREFIX + this.name + CACHE_KEY_SEPARATOR + key;
	}

	private byte[] serialize(List<?> instances) {
		Assert.isTrue(instances.size() <= MAX_SERVICE_INSTANCES,
				"Invalid service instance count in Redis cache: " + instances.size());

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

			dataOutputStream.writeInt(SERIALIZATION_VERSION);
			dataOutputStream.writeInt(instances.size());

			for (Object instance : instances) {
				Assert.isInstanceOf(ServiceInstance.class, instance,
						"LoadBalancer cache entries must contain ServiceInstance values");

				ServiceInstance serviceInstance = (ServiceInstance) instance;
				writeNullableString(dataOutputStream, serviceInstance.getInstanceId());
				writeNullableString(dataOutputStream, serviceInstance.getServiceId());
				writeNullableString(dataOutputStream, serviceInstance.getHost());
				dataOutputStream.writeInt(serviceInstance.getPort());

				URI uri = serviceInstance.getUri();
				writeNullableString(dataOutputStream, uri == null ? null : uri.toString());

				dataOutputStream.writeBoolean(serviceInstance.isSecure());

				Map<String, String> metadata = serviceInstance.getMetadata();

				if (metadata != null) {
					Assert.isTrue(metadata.size() <= MAX_METADATA_ENTRIES,
							"Invalid service instance metadata size: " + metadata.size());
				}

				dataOutputStream.writeInt(metadata == null ? 0 : metadata.size());

				if (metadata != null) {
					for (Map.Entry<String, String> entry : metadata.entrySet()) {
						writeNullableString(dataOutputStream, entry.getKey());
						writeNullableString(dataOutputStream, entry.getValue());
					}
				}
			}

			dataOutputStream.flush();
			return outputStream.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to serialize LoadBalancer cache value", ex);
		}
	}

	private List<ServiceInstance> deserialize(byte[] value) {
		try {
			DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(value));

			int version = dataInputStream.readInt();
			if (version != SERIALIZATION_VERSION) {
				throw new IllegalStateException(
						"Unsupported Redis LoadBalancer cache serialization version: " + version);
			}

			int instanceCount = dataInputStream.readInt();

			if (instanceCount < 0 || instanceCount > MAX_SERVICE_INSTANCES) {
				throw new IllegalStateException("Invalid service instance count in Redis cache: " + instanceCount);
			}

			List<ServiceInstance> instances = new ArrayList<>(instanceCount);

			for (int i = 0; i < instanceCount; i++) {
				String instanceId = readNullableString(dataInputStream);
				String serviceId = readNullableString(dataInputStream);
				String host = readNullableString(dataInputStream);
				int port = dataInputStream.readInt();
				String uri = readNullableString(dataInputStream);
				boolean secure = dataInputStream.readBoolean();

				int metadataSize = dataInputStream.readInt();
				if (metadataSize < 0 || metadataSize > MAX_METADATA_ENTRIES) {
					throw new IllegalStateException("Invalid service instance metadata size: " + metadataSize);
				}

				Map<String, String> metadata = new HashMap<>();
				for (int j = 0; j < metadataSize; j++) {
					metadata.put(readNullableString(dataInputStream), readNullableString(dataInputStream));
				}

				DefaultServiceInstance serviceInstance = new DefaultServiceInstance(instanceId, serviceId, host, port,
						secure, metadata);

				if (uri != null) {
					serviceInstance.setUri(URI.create(uri));
				}

				instances.add(serviceInstance);
			}

			if (dataInputStream.available() != 0) {
				throw new IllegalStateException("Unexpected trailing data in Redis LoadBalancer cache value");
			}

			return instances;
		}
		catch (IOException | IllegalArgumentException ex) {
			throw new IllegalStateException("Failed to deserialize LoadBalancer cache value", ex);
		}
	}

	private void writeNullableString(DataOutputStream outputStream, String value) throws IOException {
		if (value == null) {
			outputStream.writeInt(-1);
			return;
		}

		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		outputStream.writeInt(bytes.length);
		outputStream.write(bytes);
	}

	private String readNullableString(DataInputStream inputStream) throws IOException {
		int length = inputStream.readInt();

		if (length == -1) {
			return null;
		}

		if (length < 0 || length > inputStream.available()) {
			throw new IllegalStateException("Invalid string length in Redis LoadBalancer cache value");
		}

		byte[] bytes = new byte[length];
		inputStream.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

}
