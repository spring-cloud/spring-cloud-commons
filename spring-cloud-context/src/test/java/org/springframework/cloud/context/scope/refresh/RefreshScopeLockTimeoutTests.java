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

package org.springframework.cloud.context.scope.refresh;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.awaitility.Awaitility.await;

/**
 * Reproduces a queued refresh writer blocking a dependent reader (gh-1589) and verifies
 * bounded refresh lock acquisition (gh-1401).
 *
 * @author 施浩
 */
class RefreshScopeLockTimeoutTests {

	private final ExecutorService executor = Executors.newFixedThreadPool(3);

	private final CountDownLatch entered = new CountDownLatch(1);

	private final CountDownLatch release = new CountDownLatch(1);

	private final AtomicReference<Thread> writerThread = new AtomicReference<>();

	private final AtomicInteger refreshEvents = new AtomicInteger();

	private AnnotationConfigApplicationContext context;

	private TestScope scope;

	private Service service;

	private Service target;

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.scope = this.context.getBean(TestScope.class);
		this.service = this.context.getBean(Service.class);
		this.service.identity();
		this.target = (Service) ((ScopedObject) this.service).getTargetObject();
		this.context.addApplicationListener(event -> {
			if (event instanceof RefreshScopeRefreshedEvent) {
				this.refreshEvents.incrementAndGet();
			}
		});
	}

	@AfterEach
	void tearDown() throws InterruptedException {
		// Break the application-level wait even if an assertion fails, including on the
		// unbounded implementation. Do not leave blocked threads or hang context close.
		this.release.countDown();
		this.executor.shutdown();
		assertThat(this.executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		this.context.close();
	}

	@Test
	void defaultRefreshCanBlockAReaderNeededByAnActiveInvocation() throws Exception {
		assertThat(this.scope.getLockTimeout()).isNull();
		Future<Object> holder = holdReadLock();
		Future<?> writer = refreshInBackground(true);
		Future<Object> worker = startDependentReader();

		assertThat(holder.isDone()).isFalse();
		assertThat(writer.isDone()).isFalse();
		assertThat(worker.isDone()).isFalse();

		// Without intervention A waits for B, B waits behind C's queued writer, and C
		// waits for A's read lock. Release A explicitly so the test can clean up.
		this.release.countDown();
		holder.get(10, TimeUnit.SECONDS);
		writer.get(10, TimeUnit.SECONDS);
		worker.get(10, TimeUnit.SECONDS);
		assertThat(this.target.destructions).hasValue(1);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void timeoutReleasesQueuedWriterAndRetainsTarget(boolean all) throws Exception {
		this.scope.setLockTimeout(Duration.ofSeconds(3));
		Future<Object> holder = holdReadLock();
		Future<?> writer = refreshInBackground(all);
		Future<Object> worker = startDependentReader();

		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> writer.get(10, TimeUnit.SECONDS))
			.withCauseInstanceOf(IllegalStateException.class)
			.havingCause()
			.withMessageContaining("scopedTarget.service")
			.withMessageContaining("Timed out");
		assertThat(worker.get(10, TimeUnit.SECONDS)).isSameAs(this.target.identity);
		assertThat(holder.get(10, TimeUnit.SECONDS)).isSameAs(this.target.identity);
		assertThat(this.service.identity()).isSameAs(this.target.identity);
		assertThat(this.target.destructions).hasValue(0);
		assertThat(this.refreshEvents).hasValue(0);

		if (all) {
			this.scope.refreshAll();
		}
		else {
			assertThat(this.scope.refresh("service")).isTrue();
		}
		assertThat(this.target.destructions).hasValue(1);
		assertThat(this.service.identity()).isNotSameAs(this.target.identity);
		assertThat(this.refreshEvents).hasValue(1);
	}

	@Test
	void interruptedWriterRetainsTargetAndInterruptStatus() throws Exception {
		this.scope.setLockTimeout(Duration.ofSeconds(30));
		Future<Object> holder = holdReadLock();
		AtomicBoolean interrupted = new AtomicBoolean();
		Future<?> writer = this.executor.submit(() -> {
			this.writerThread.set(Thread.currentThread());
			try {
				this.scope.refreshAll();
			}
			finally {
				interrupted.set(Thread.currentThread().isInterrupted());
			}
		});
		awaitWriter();
		this.writerThread.get().interrupt();

		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> writer.get(10, TimeUnit.SECONDS))
			.withCauseInstanceOf(IllegalStateException.class)
			.havingCause()
			.withCauseInstanceOf(InterruptedException.class);
		assertThat(interrupted).isTrue();
		this.release.countDown();
		holder.get(10, TimeUnit.SECONDS);
		assertThat(this.service.identity()).isSameAs(this.target.identity);
		assertThat(this.target.destructions).hasValue(0);
		assertThat(this.refreshEvents).hasValue(0);
		this.scope.refreshAll();
		assertThat(this.target.destructions).hasValue(1);
	}

	@Test
	void zeroTimeoutDoesNotEvictBusyTarget() throws Exception {
		this.scope.setLockTimeout(Duration.ZERO);
		Future<Object> holder = holdReadLock();
		assertThatIllegalStateException().isThrownBy(() -> this.scope.refresh("service"))
			.withMessageContaining("scopedTarget.service");
		this.release.countDown();
		holder.get(10, TimeUnit.SECONDS);
		assertThat(this.service.identity()).isSameAs(this.target.identity);
		assertThat(this.target.destructions).hasValue(0);
		assertThat(this.refreshEvents).hasValue(0);
		assertThat(this.scope.refresh("service")).isTrue();
		assertThat(this.target.destructions).hasValue(1);
	}

	@Test
	void missingTargetDoesNotPublishRefreshEvent() {
		this.scope.setLockTimeout(Duration.ZERO);
		assertThat(this.scope.refresh("missing")).isFalse();
		assertThat(this.refreshEvents).hasValue(0);
	}

	@Test
	void rejectsNegativeTimeoutAndAllowsRestoringDefault() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.scope.setLockTimeout(Duration.ofMillis(-1)));
		this.scope.setLockTimeout(Duration.ZERO);
		this.scope.setLockTimeout(null);
		assertThat(this.scope.getLockTimeout()).isNull();
	}

	@Test
	void shutdownStillWaitsForActiveInvocations() throws Exception {
		this.scope.setLockTimeout(Duration.ZERO);
		Future<Object> holder = holdReadLock();
		Future<?> shutdown = this.executor.submit(() -> {
			this.writerThread.set(Thread.currentThread());
			this.context.close();
		});
		awaitWriter();
		assertThat(shutdown.isDone()).isFalse();
		assertThat(this.target.destructions).hasValue(0);
		this.release.countDown();
		holder.get(10, TimeUnit.SECONDS);
		shutdown.get(10, TimeUnit.SECONDS);
		assertThat(this.target.destructions).hasValue(1);
	}

	private Future<Object> holdReadLock() throws InterruptedException {
		Future<Object> holder = this.executor.submit(() -> this.service.block(this.entered, this.release));
		assertThat(this.entered.await(5, TimeUnit.SECONDS)).isTrue();
		return holder;
	}

	private Future<?> refreshInBackground(boolean all) {
		Future<?> writer = this.executor.submit(() -> {
			this.writerThread.set(Thread.currentThread());
			if (all) {
				this.scope.refreshAll();
			}
			else {
				this.scope.refresh("service");
			}
		});
		awaitWriter();
		return writer;
	}

	private void awaitWriter() {
		await().atMost(Duration.ofSeconds(5))
			.until(() -> this.writerThread.get() != null && this.scope.lock().hasQueuedThread(this.writerThread.get()));
	}

	private Future<Object> startDependentReader() {
		AtomicReference<Thread> readerThread = new AtomicReference<>();
		Future<Object> worker = this.executor.submit(() -> {
			readerThread.set(Thread.currentThread());
			try {
				return this.service.identity();
			}
			finally {
				this.release.countDown();
			}
		});
		await().atMost(Duration.ofSeconds(2))
			.until(() -> readerThread.get() != null && this.scope.lock().hasQueuedThread(readerThread.get()));
		return worker;
	}

	static class TestScope extends RefreshScope {

		ReentrantReadWriteLock lock() {
			return (ReentrantReadWriteLock) getLock("scopedTarget.service");
		}

	}

	static class Service implements DisposableBean {

		private final AtomicInteger destructions = new AtomicInteger();

		private final Object identity = new Object();

		public Object identity() {
			return this.identity;
		}

		public Object block(CountDownLatch entered, CountDownLatch release) throws InterruptedException {
			entered.countDown();
			if (!release.await(20, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Test invocation was not released");
			}
			return this.identity;
		}

		@Override
		public void destroy() {
			this.destructions.incrementAndGet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		static TestScope refreshScope() {
			return new TestScope();
		}

		@Bean
		@org.springframework.cloud.context.config.annotation.RefreshScope
		Service service() {
			return new Service();
		}

	}

}
