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

package org.springframework.cloud.context.scope;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.context.scope.thread.ThreadLocalScopeCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for gh-630: {@link GenericScope} proxy locking must be driven by the
 * {@link ScopeCache} in use, so that caches whose instances are confined to a single
 * thread (e.g. {@link ThreadLocalScopeCache}) are not penalized by name-keyed read/write
 * locks shared across all scope instances.
 */
class GenericScopeLockingTests {

	@Test
	void standardScopeCacheRequiresLockingByDefault() {
		assertThat(new StandardScopeCache().requiresLocking()).isTrue();
	}

	@Test
	void threadLocalScopeCacheOptsOutOfLocking() {
		assertThat(new ThreadLocalScopeCache().requiresLocking()).isFalse();
	}

	@Test
	void proxiedInvocationsConsultLockWhenCacheRequiresIt() {
		new ApplicationContextRunner().withUserConfiguration(StandardCacheConfig.class).run((context) -> {
			Service service = context.getBean(Service.class);
			assertThat(service.ping()).isEqualTo("pong");
			CountingScope scope = context.getBean(CountingScope.class);
			assertThat(scope.lockConsultations.get()).isEqualTo(1);
		});
	}

	@Test
	void proxiedInvocationsSkipLockWhenCacheDoesNotRequireIt() {
		new ApplicationContextRunner().withUserConfiguration(ThreadLocalCacheConfig.class).run((context) -> {
			Service service = context.getBean(Service.class);
			assertThat(service.ping()).isEqualTo("pong");
			CountingScope scope = context.getBean(CountingScope.class);
			assertThat(scope.lockConsultations.get()).isZero();
		});
	}

	@Test
	void beanCreationInOneScopeInstanceDoesNotBlockOtherInstancesWithSameBeanName() throws Exception {
		GenericScope first = new GenericScope();
		GenericScope second = new GenericScope();
		CountDownLatch creationStarted = new CountDownLatch(1);
		CountDownLatch releaseCreation = new CountDownLatch(1);
		AtomicReference<Object> firstResult = new AtomicReference<>();
		Thread creator = new Thread(() -> firstResult.set(first.get("shared-name", () -> {
			creationStarted.countDown();
			try {
				releaseCreation.await(30, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ex);
			}
			return new Object();
		})));
		creator.start();
		assertThat(creationStarted.await(5, TimeUnit.SECONDS)).isTrue();

		// While the first scope instance is creating its "shared-name" bean, a second,
		// independent scope instance must be able to create its own instance of the
		// same bean name without waiting on the first (gh-630).
		AtomicReference<Object> secondResult = new AtomicReference<>(null);
		CountDownLatch secondDone = new CountDownLatch(1);
		Thread independent = new Thread(() -> {
			secondResult.set(second.get("shared-name", Object::new));
			secondDone.countDown();
		});
		independent.start();

		boolean unblocked = secondDone.await(10, TimeUnit.SECONDS);

		releaseCreation.countDown();
		creator.join(TimeUnit.SECONDS.toMillis(5));
		independent.join(TimeUnit.SECONDS.toMillis(5));

		assertThat(unblocked).as("second scope was blocked by first scope's creation").isTrue();
		assertThat(secondResult.get()).isNotNull();
		assertThat(firstResult.get()).isNotNull();
	}

	interface Service {

		String ping();

	}

	static class SimpleService implements Service {

		@Override
		public String ping() {
			return "pong";
		}

	}

	static class CountingScope extends GenericScope {

		final AtomicInteger lockConsultations = new AtomicInteger();

		@Override
		protected ReadWriteLock getLock(String beanName) {
			this.lockConsultations.incrementAndGet();
			return super.getLock(beanName);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StandardCacheConfig {

		@Bean
		static CountingScope genericScope() {
			CountingScope scope = new CountingScope();
			scope.setName("standard-cache-scope");
			scope.setScopeCache(new StandardScopeCache());
			return scope;
		}

		@Bean
		@Scope(value = "standard-cache-scope", proxyMode = ScopedProxyMode.TARGET_CLASS)
		Service service() {
			return new SimpleService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ThreadLocalCacheConfig {

		@Bean
		static CountingScope genericScope() {
			CountingScope scope = new CountingScope();
			scope.setName("thread-local-scope");
			scope.setScopeCache(new ThreadLocalScopeCache());
			return scope;
		}

		@Bean
		@Scope(value = "thread-local-scope", proxyMode = ScopedProxyMode.TARGET_CLASS)
		Service service() {
			return new SimpleService();
		}

	}

}
