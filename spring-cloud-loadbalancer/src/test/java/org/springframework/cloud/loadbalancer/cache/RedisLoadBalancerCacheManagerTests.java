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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cache.Cache;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RedisLoadBalancerCacheManager}.
 *
 * @author
 */
class RedisLoadBalancerCacheManagerTests {

	private static final int REDIS_PORT = 6379;

	private static final String CACHE_NAME = "CachingServiceInstanceListSupplierCache";

	private static final String REDIS_KEY_PREFIX = "spring-cloud-loadbalancer:";

	private static final int SERIALIZATION_VERSION = 1;

	private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
		.withExposedPorts(REDIS_PORT);

	private static LettuceConnectionFactory connectionFactory;

	private RedisLoadBalancerCacheManager cacheManager;

	@BeforeAll
	static void startRedis() {
		REDIS.start();
		connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
		connectionFactory.afterPropertiesSet();
	}

	@AfterAll
	static void stopRedis() {
		if (connectionFactory != null) {
			connectionFactory.destroy();
		}
		REDIS.stop();
	}

	@BeforeEach
	void setUp() {
		LoadBalancerCacheProperties properties = new LoadBalancerCacheProperties();
		properties.setTtl(Duration.ofSeconds(5));
		this.cacheManager = new RedisLoadBalancerCacheManager(connectionFactory, properties);
		this.cacheManager.afterPropertiesSet();

		Cache cache = this.cacheManager.getCache(CACHE_NAME);
		assertThat(cache).isNotNull();
		cache.clear();
	}

	@Test
	void shouldStoreAndRetrieveServiceInstances() {
		Cache cache = getServiceInstanceCache();
		ServiceInstance instance = new DefaultServiceInstance("instance-1", "orders", "localhost", 8080, false,
				Map.of("zone", "east"));

		cache.put("orders", List.of(instance));

		Cache.ValueWrapper result = cache.get("orders");

		assertThat(result).isNotNull();
		assertThat(result.get()).isInstanceOf(List.class);

		List<?> instances = (List<?>) result.get();
		assertThat(instances).hasSize(1);

		ServiceInstance retrieved = (ServiceInstance) instances.get(0);
		assertThat(retrieved.getInstanceId()).isEqualTo("instance-1");
		assertThat(retrieved.getServiceId()).isEqualTo("orders");
		assertThat(retrieved.getHost()).isEqualTo("localhost");
		assertThat(retrieved.getPort()).isEqualTo(8080);
		assertThat(retrieved.getMetadata()).containsEntry("zone", "east");
	}

	@Test
	void shouldReturnNullForMissingEntry() {
		assertThat(getServiceInstanceCache().get("missing")).isNull();
	}

	@Test
	void shouldEvictEntry() {
		Cache cache = getServiceInstanceCache();
		cache.put("orders", List.of(createInstance()));

		cache.evict("orders");

		assertThat(cache.get("orders")).isNull();
	}

	@Test
	void shouldSupportPutIfAbsent() {
		Cache cache = getServiceInstanceCache();

		assertThat(cache.putIfAbsent("orders", List.of(createInstance()))).isNull();
		assertThat(cache.putIfAbsent("orders", List.of(createInstance()))).isNotNull();
	}

	@Test
	void shouldExpireEntriesAfterConfiguredTtl() throws InterruptedException {
		Cache cache = getServiceInstanceCache();
		cache.put("orders", List.of(createInstance()));

		Thread.sleep(6000);

		assertThat(cache.get("orders")).isNull();
	}

	@Test
	void shouldPreserveServiceInstanceUriAfterSerialization() {
		Cache cache = getServiceInstanceCache();
		ServiceInstance instance = createInstance();

		cache.put("orders", List.of(instance));

		Cache.ValueWrapper result = cache.get("orders");

		assertThat(result).isNotNull();

		List<?> instances = (List<?>) result.get();
		assertThat(instances).hasSize(1);

		ServiceInstance retrieved = (ServiceInstance) instances.get(0);

		assertThat(retrieved.getUri()).isEqualTo(URI.create("http://localhost:8080"));
		assertThat(retrieved.getInstanceId()).isEqualTo("instance-1");
		assertThat(retrieved.getServiceId()).isEqualTo("orders");
		assertThat(retrieved.getMetadata()).isEmpty();
	}

	@Test
	void shouldRejectUnsupportedSerializationVersion() throws IOException {
		byte[] invalidValue = createPayload(outputStream -> {
			outputStream.writeInt(99);
			outputStream.writeInt(0);
		});

		assertInvalidPayload(invalidValue, "Unsupported Redis LoadBalancer cache serialization version");
	}

	@Test
	void shouldRejectTruncatedPayload() {
		byte[] invalidValue = { 0, 0, 0, SERIALIZATION_VERSION, 0 };

		assertInvalidPayload(invalidValue, "Failed to deserialize LoadBalancer cache value");
	}

	@Test
	void shouldRejectInvalidServiceInstanceCount() throws IOException {
		byte[] invalidValue = createPayload(outputStream -> {
			outputStream.writeInt(SERIALIZATION_VERSION);
			outputStream.writeInt(-1);
		});

		assertInvalidPayload(invalidValue, "Invalid service instance count in Redis cache");
	}

	@Test
	void shouldRejectInvalidMetadataCount() throws IOException {
		byte[] invalidValue = createPayload(outputStream -> {
			outputStream.writeInt(SERIALIZATION_VERSION);
			outputStream.writeInt(1);

			writeString(outputStream, "instance-1");
			writeString(outputStream, "orders");
			writeString(outputStream, "localhost");
			outputStream.writeInt(8080);
			writeString(outputStream, "http://localhost:8080");
			outputStream.writeBoolean(false);

			outputStream.writeInt(-1);
		});

		assertInvalidPayload(invalidValue, "Invalid service instance metadata size");
	}

	@Test
	void shouldRejectInvalidStringLength() throws IOException {
		byte[] invalidValue = createPayload(outputStream -> {
			outputStream.writeInt(SERIALIZATION_VERSION);
			outputStream.writeInt(1);

			outputStream.writeInt(-2);
		});

		assertInvalidPayload(invalidValue, "Invalid string length in Redis LoadBalancer cache value");
	}

	@Test
	void shouldRejectMalformedServiceInstanceUri() throws IOException {
		byte[] invalidValue = createPayload(outputStream -> {
			outputStream.writeInt(SERIALIZATION_VERSION);
			outputStream.writeInt(1);

			writeString(outputStream, "instance-1");
			writeString(outputStream, "orders");
			writeString(outputStream, "localhost");
			outputStream.writeInt(8080);
			writeString(outputStream, "://invalid-uri");
			outputStream.writeBoolean(false);
			outputStream.writeInt(0);
		});

		assertInvalidPayload(invalidValue, "Failed to deserialize LoadBalancer cache value");
	}

	@Test
	void shouldRejectUnexpectedTrailingData() throws IOException {
		byte[] invalidValue = createPayload(outputStream -> {
			outputStream.writeInt(SERIALIZATION_VERSION);
			outputStream.writeInt(0);
			outputStream.writeByte(1);
		});

		assertInvalidPayload(invalidValue, "Unexpected trailing data in Redis LoadBalancer cache value");
	}

	private void assertInvalidPayload(byte[] invalidValue, String expectedMessage) {
		Cache cache = getServiceInstanceCache();
		writeRedisValue("orders", invalidValue);

		assertThatThrownBy(() -> cache.get("orders")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(expectedMessage);
	}

	private void writeRedisValue(String key, byte[] value) {
		@SuppressWarnings("unchecked")
		RedisTemplate<String, byte[]> redisTemplate = (RedisTemplate<String, byte[]>) getServiceInstanceCache()
			.getNativeCache();

		String redisKey = REDIS_KEY_PREFIX + CACHE_NAME + ":" + key;
		redisTemplate.opsForValue().set(redisKey, value);
	}

	private byte[] createPayload(PayloadWriter writer) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
			writer.write(dataOutputStream);
		}
		return outputStream.toByteArray();
	}

	private void writeString(DataOutputStream outputStream, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		outputStream.writeInt(bytes.length);
		outputStream.write(bytes);
	}

	private Cache getServiceInstanceCache() {
		return this.cacheManager.getCache(CACHE_NAME);
	}

	private ServiceInstance createInstance() {
		return new DefaultServiceInstance("instance-1", "orders", "localhost", 8080, false, Map.of());
	}

	@FunctionalInterface
	private interface PayloadWriter {

		void write(DataOutputStream outputStream) throws IOException;

	}

}
