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

package org.springframework.cloud.commons.publisher;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Tim Ysewyn
 */
public class FluxFirstNonEmptyEmittingTests {

	@Test
	public void arrayNull() {
		assertThatNullPointerException().isThrownBy(() -> CloudFlux.firstNonEmpty((Publisher<Integer>[]) null));
	}

	@Test
	public void iterableNull() {
		assertThatNullPointerException().isThrownBy(() -> CloudFlux.firstNonEmpty((Iterable<Publisher<Integer>>) null));
	}

	@Test
	public void firstWinner() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.range(11, 10)))
				.expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).verifyComplete();
	}

	@Test
	public void firstWinnerSecondEmpty() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.empty()))
				.expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).verifyComplete();
	}

	@Test
	public void firstWinnerBackpressured() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.range(11, 10))).thenRequest(5)
				.expectNext(1, 2, 3, 4, 5).thenCancel().verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void secondWinner() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), Flux.range(11, 10).log()))
				.expectNext(11, 12, 13, 14, 15, 16, 17, 18, 19, 20).verifyComplete();
	}

	@Test
	public void secondWinnerFirstEmpty() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.empty(), Flux.range(11, 10).log()))
				.expectNext(11, 12, 13, 14, 15, 16, 17, 18, 19, 20).verifyComplete();
	}

	@Test
	public void bothEmpty() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.empty(), Flux.empty())).expectComplete().verifyThenAssertThat()
				.hasNotDiscardedElements().hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void neverAndEmpty() {
		StepVerifier.withVirtualTime(() -> CloudFlux.firstNonEmpty(Flux.never(), Flux.empty())).expectSubscription()
				.expectNoEvent(Duration.ofDays(1)).thenCancel().verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void firstEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.<Integer>error(ex), Flux.empty()))
				.expectErrorMessage("forced failure").verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void secondEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.empty(), Flux.<Integer>error(ex)))
				.expectErrorMessage("forced failure").verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void neverAndSecondEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), Flux.<Integer>error(ex)))
				.expectErrorMessage("forced failure").verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void singleArrayNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty((Publisher<Object>) null)).expectError(NullPointerException.class)
				.verify();
	}

	@Test
	public void arrayOneIsNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), null, Flux.never()))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void singleIterableNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty(singletonList((Publisher<Object>) null)))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void iterableOneIsNullSource() {
		StepVerifier
				.create(CloudFlux.firstNonEmpty(Arrays.asList(Flux.never(), (Publisher<Object>) null, Flux.never())))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void scanSubscriber() {
		CoreSubscriber<String> actual = new LambdaSubscriber<>(null, e -> {
		}, null, null);
		FluxFirstNonEmptyEmitting.RaceCoordinator<String> parent = new FluxFirstNonEmptyEmitting.RaceCoordinator<>(1);
		FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<String> test = new FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<>(
				actual, parent, 1);
		Subscription sub = Operators.emptySubscription();
		test.onSubscribe(sub);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(sub);
		assertThat(test.scan(Scannable.Attr.ACTUAL)).isSameAs(actual);
		assertThat(test.scan(Scannable.Attr.CANCELLED)).isFalse();
		parent.cancelled = true;
		assertThat(test.scan(Scannable.Attr.CANCELLED)).isTrue();
	}

	@Test
	public void scanRaceCoordinator() {
		CoreSubscriber<String> actual = new LambdaSubscriber<>(null, e -> {
		}, null, null);
		FluxFirstNonEmptyEmitting.RaceCoordinator<String> parent = new FluxFirstNonEmptyEmitting.RaceCoordinator<>(1);
		FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<String> test = new FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<>(
				actual, parent, 1);
		Subscription sub = Operators.emptySubscription();
		test.onSubscribe(sub);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(sub);
		assertThat(test.scan(Scannable.Attr.ACTUAL)).isSameAs(actual);
		assertThat(parent.scan(Scannable.Attr.CANCELLED)).isFalse();
		parent.cancelled = true;
		assertThat(parent.scan(Scannable.Attr.CANCELLED)).isTrue();
	}

	static class LambdaSubscriber<T> implements CoreSubscriber<T>, Scannable, Disposable {

		final Consumer<? super T> consumer;

		final Consumer<? super Throwable> errorConsumer;

		final Runnable completeConsumer;

		final Consumer<? super Subscription> subscriptionConsumer;

		final Context initialContext;

		volatile Subscription subscription;
		static final AtomicReferenceFieldUpdater<LambdaSubscriber, Subscription> S = AtomicReferenceFieldUpdater
				.newUpdater(LambdaSubscriber.class, Subscription.class, "subscription");

		/**
		 * Create a {@link Subscriber} reacting onNext, onError and onComplete. If no
		 * {@code subscriptionConsumer} is provided, the subscriber will automatically
		 * request Long.MAX_VALUE in onSubscribe, as well as an initial {@link Context}
		 * that will be visible by operators upstream in the chain.
		 * @param consumer A {@link Consumer} with argument onNext data
		 * @param errorConsumer A {@link Consumer} called onError
		 * @param completeConsumer A {@link Runnable} called onComplete with the actual
		 * context if any
		 * @param subscriptionConsumer A {@link Consumer} called with the
		 * {@link Subscription} to perform initial request, or null to request max
		 * @param initialContext A {@link Context} for this subscriber, or null to use the
		 * default of an {@link Context#empty() empty Context}.
		 */
		LambdaSubscriber(@Nullable Consumer<? super T> consumer, @Nullable Consumer<? super Throwable> errorConsumer,
				@Nullable Runnable completeConsumer, @Nullable Consumer<? super Subscription> subscriptionConsumer,
				@Nullable Context initialContext) {
			this.consumer = consumer;
			this.errorConsumer = errorConsumer;
			this.completeConsumer = completeConsumer;
			this.subscriptionConsumer = subscriptionConsumer;
			this.initialContext = initialContext == null ? Context.empty() : initialContext;
		}

		/**
		 * Create a {@link Subscriber} reacting onNext, onError and onComplete. If no
		 * {@code subscriptionConsumer} is provided, the subscriber will automatically
		 * request Long.MAX_VALUE in onSubscribe, as well as an initial {@link Context}
		 * that will be visible by operators upstream in the chain.
		 * @param consumer A {@link Consumer} with argument onNext data
		 * @param errorConsumer A {@link Consumer} called onError
		 * @param completeConsumer A {@link Runnable} called onComplete with the actual
		 * context if any
		 * @param subscriptionConsumer A {@link Consumer} called with the
		 * {@link Subscription} to perform initial request, or null to request max
		 */ // left mainly for the benefit of tests
		LambdaSubscriber(@Nullable Consumer<? super T> consumer, @Nullable Consumer<? super Throwable> errorConsumer,
				@Nullable Runnable completeConsumer, @Nullable Consumer<? super Subscription> subscriptionConsumer) {
			this(consumer, errorConsumer, completeConsumer, subscriptionConsumer, null);
		}

		@Override
		public String stepName() {
			// /!\ this code is duplicated in `Scannable#stepName` in order to use
			// toString instead of simple class name

			/*
			 * Strip an operator name of various prefixes and suffixes.
			 *
			 * @param name the operator name, usually simpleClassName or fully-qualified
			 * classname.
			 *
			 * @return the stripped operator name
			 */
			String name = getClass().getSimpleName();
			if (name.contains("@") && name.contains("$")) {
				name = name.substring(0, name.indexOf('$')).substring(name.lastIndexOf('.') + 1);
			}
			String stripped = OPERATOR_NAME_UNRELATED_WORDS_PATTERN.matcher(name).replaceAll("");

			if (!stripped.isEmpty()) {
				return stripped.substring(0, 1).toLowerCase() + stripped.substring(1);
			}
			return stripped;
		}

		@Override
		public Context currentContext() {
			return this.initialContext;
		}

		@Override
		public final void onSubscribe(Subscription s) {
			if (Operators.validate(subscription, s)) {
				this.subscription = s;
				if (subscriptionConsumer != null) {
					try {
						subscriptionConsumer.accept(s);
					}
					catch (Throwable t) {
						Exceptions.throwIfFatal(t);
						s.cancel();
						onError(t);
					}
				}
				else {
					s.request(Long.MAX_VALUE);
				}
			}
		}

		@Override
		public final void onComplete() {
			Subscription s = S.getAndSet(this, Operators.cancelledSubscription());
			if (s == Operators.cancelledSubscription()) {
				return;
			}
			if (completeConsumer != null) {
				try {
					completeConsumer.run();
				}
				catch (Throwable t) {
					Exceptions.throwIfFatal(t);
					onError(t);
				}
			}
		}

		@Override
		public final void onError(Throwable t) {
			Subscription s = S.getAndSet(this, Operators.cancelledSubscription());
			if (s == Operators.cancelledSubscription()) {
				Operators.onErrorDropped(t, this.initialContext);
				return;
			}
			if (errorConsumer != null) {
				errorConsumer.accept(t);
			}
			else {
				Operators.onErrorDropped(Exceptions.errorCallbackNotImplemented(t), this.initialContext);
			}
		}

		@Override
		public final void onNext(T x) {
			try {
				if (consumer != null) {
					consumer.accept(x);
				}
			}
			catch (Throwable t) {
				Exceptions.throwIfFatal(t);
				this.subscription.cancel();
				onError(t);
			}
		}

		@Override
		@Nullable
		public Object scanUnsafe(Attr key) {
			if (key == Attr.PARENT) {
				return subscription;
			}
			if (key == Attr.PREFETCH) {
				return Integer.MAX_VALUE;
			}
			if (key == Attr.TERMINATED || key == Attr.CANCELLED) {
				return isDisposed();
			}
			/*
			 * if (key == Attr.RUN_STYLE) { return Attr.RunStyle.SYNC; }
			 */

			return null;
		}

		@Override
		public boolean isDisposed() {
			return subscription == Operators.cancelledSubscription();
		}

		@Override
		public void dispose() {
			Subscription s = S.getAndSet(this, Operators.cancelledSubscription());
			if (s != null && s != Operators.cancelledSubscription()) {
				s.cancel();
			}
		}

	}

}
