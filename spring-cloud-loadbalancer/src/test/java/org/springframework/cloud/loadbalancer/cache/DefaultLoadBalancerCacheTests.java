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

package org.springframework.cloud.loadbalancer.cache;

import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultLoadBalancerCache}.
 *
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(MockitoExtension.class)
class DefaultLoadBalancerCacheTests {

	@Test
	void shouldAllowNullValuesByDefault() {
		DefaultLoadBalancerCache cache = new DefaultLoadBalancerCache("test");

		assertThatCode(() -> cache.put("testKey", null)).doesNotThrowAnyException();
	}

	@Test
	void shouldThrowExceptionIfNullPutWithNonNullSetup() {
		DefaultLoadBalancerCache cache = new DefaultLoadBalancerCache("test", false);

		assertThatIllegalArgumentException().isThrownBy(() -> cache.put("testKey", null))
				.withMessageContaining(
						"Cache 'test' is configured to not allow null values but null was provided");
	}

	@Test
	void shouldNotEvictEntriesByDefault() {
		DefaultLoadBalancerCache cache = new DefaultLoadBalancerCache("test");

		assertThat(cache.getEvictMs()).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	@Test
	void assertThatTtlApplied() {
		ConcurrentHashMapWithTimedEviction nativeCache = mock(
				ConcurrentHashMapWithTimedEviction.class);
		DefaultLoadBalancerCache cache = new DefaultLoadBalancerCache("test", nativeCache,
				50, true);

		cache.put("testKey", "testValue");

		verify(nativeCache, times(1)).put("testKey", "testValue", 50);
	}

}
