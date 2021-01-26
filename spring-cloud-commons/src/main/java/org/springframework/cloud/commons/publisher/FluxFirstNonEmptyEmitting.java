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

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.util.annotation.Nullable;

/**
 * @author Tim Ysewyn
 */
final class FluxFirstNonEmptyEmitting<T> extends Flux<T> implements Scannable, Publisher<T> {

	final Publisher<? extends T>[] array;

	final Iterable<? extends Publisher<? extends T>> iterable;

	@SafeVarargs
	FluxFirstNonEmptyEmitting(Publisher<? extends T>... array) {
		this.array = Objects.requireNonNull(array, "array");
		this.iterable = null;
	}

	FluxFirstNonEmptyEmitting(Iterable<? extends Publisher<? extends T>> iterable) {
		this.array = null;
		this.iterable = Objects.requireNonNull(iterable);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void subscribe(CoreSubscriber<? super T> actual) {
		Publisher<? extends T>[] a = array;
		int n;
		if (a == null) {
			n = 0;
			a = new Publisher[8];

			Iterator<? extends Publisher<? extends T>> it;

			try {
				it = Objects.requireNonNull(iterable.iterator(), "The iterator returned is null");
			}
			catch (Throwable e) {
				Operators.error(actual, Operators.onOperatorError(e, actual.currentContext()));
				return;
			}

			for (;;) {

				boolean b;

				try {
					b = it.hasNext();
				}
				catch (Throwable e) {
					Operators.error(actual, Operators.onOperatorError(e, actual.currentContext()));
					return;
				}

				if (!b) {
					break;
				}

				Publisher<? extends T> p;

				try {
					p = Objects.requireNonNull(it.next(), "The Publisher returned by the iterator is null");
				}
				catch (Throwable e) {
					Operators.error(actual, Operators.onOperatorError(e, actual.currentContext()));
					return;
				}

				if (n == a.length) {
					Publisher<? extends T>[] c = new Publisher[n + (n >> 2)];
					System.arraycopy(a, 0, c, 0, n);
					a = c;
				}
				a[n++] = p;
			}

		}
		else {
			n = a.length;
		}

		if (n == 0) {
			Operators.complete(actual);
			return;
		}
		if (n == 1) {
			Publisher<? extends T> p = a[0];

			if (p == null) {
				Operators.error(actual, new NullPointerException("The single source Publisher is null"));
			}
			else {
				p.subscribe(actual);
			}
			return;
		}

		RaceCoordinator<T> coordinator = new RaceCoordinator<>(n);

		coordinator.subscribe(a, n, actual);
	}

	@Override
	public Object scanUnsafe(Attr key) {
		return null; // no particular key to be represented, still useful in hooks
	}

	@Override
	public String stepName() {
		return "source(" + getClass().getSimpleName() + ")";
	}

	static final class RaceCoordinator<T> implements Subscription, Scannable {

		final FirstNonEmptyEmittingSubscriber<T>[] subscribers;

		volatile boolean cancelled;

		volatile int wip;

		volatile int competingSubscribers;

		@SuppressWarnings("rawtypes")
		static final AtomicIntegerFieldUpdater<RaceCoordinator> WIP = AtomicIntegerFieldUpdater
				.newUpdater(RaceCoordinator.class, "wip");

		static final AtomicIntegerFieldUpdater<RaceCoordinator> COMPETING_SUBSCRIBERS = AtomicIntegerFieldUpdater
				.newUpdater(RaceCoordinator.class, "competingSubscribers");

		@SuppressWarnings("unchecked")
		RaceCoordinator(int n) {
			subscribers = new FirstNonEmptyEmittingSubscriber[n];
			wip = Integer.MIN_VALUE;
			competingSubscribers = n;
		}

		@Override
		public Stream<? extends Scannable> inners() {
			return Stream.of(subscribers);
		}

		@Override
		@Nullable
		public Object scanUnsafe(Attr key) {
			if (key == Attr.CANCELLED) {
				return cancelled;
			}

			return null;
		}

		void subscribe(Publisher<? extends T>[] sources, int n, CoreSubscriber<? super T> actual) {
			FirstNonEmptyEmittingSubscriber<T>[] a = subscribers;

			for (int i = 0; i < n; i++) {
				a[i] = new FirstNonEmptyEmittingSubscriber<>(actual, this, i);
			}

			actual.onSubscribe(this);

			for (int i = 0; i < n; i++) {
				if (cancelled || wip != Integer.MIN_VALUE) {
					return;
				}

				Publisher<? extends T> p = sources[i];

				if (p == null) {
					if (WIP.compareAndSet(this, Integer.MIN_VALUE, -1)) {
						actual.onError(new NullPointerException("The " + i + " th Publisher source is null"));
					}
					return;
				}

				p.subscribe(a[i]);
			}

		}

		@Override
		public void request(long n) {
			if (Operators.validate(n)) {
				int w = wip;
				if (w >= 0) {
					subscribers[w].request(n);
				}
				else {
					for (FirstNonEmptyEmittingSubscriber<T> s : subscribers) {
						s.request(n);
					}
				}
			}
		}

		@Override
		public void cancel() {
			if (cancelled) {
				return;
			}
			cancelled = true;

			int w = wip;
			if (w >= 0) {
				subscribers[w].cancel();
			}
			else {
				for (FirstNonEmptyEmittingSubscriber<T> s : subscribers) {
					s.cancel();
				}
			}
		}

		boolean tryWin(int index) {
			if (wip == Integer.MIN_VALUE) {
				if (WIP.compareAndSet(this, Integer.MIN_VALUE, index)) {

					FirstNonEmptyEmittingSubscriber<T>[] a = subscribers;
					int n = a.length;

					for (int i = 0; i < n; i++) {
						if (i != index) {
							a[i].cancel();
						}
					}

					return true;
				}
			}
			return false;
		}

		int resignFromRace() {
			return COMPETING_SUBSCRIBERS.decrementAndGet(this);
		}

	}

	static final class FirstNonEmptyEmittingSubscriber<T> extends Operators.DeferredSubscription
			implements CoreSubscriber<T>, Scannable, Subscription {

		final RaceCoordinator<T> parent;

		final CoreSubscriber<? super T> actual;

		final int index;

		boolean won;

		FirstNonEmptyEmittingSubscriber(CoreSubscriber<? super T> actual, RaceCoordinator<T> parent, int index) {
			this.actual = actual;
			this.parent = parent;
			this.index = index;
		}

		@Override
		@Nullable
		public Object scanUnsafe(Attr key) {
			if (key == Attr.ACTUAL) {
				return actual;
			}
			if (key == Attr.CANCELLED) {
				return parent.cancelled;
			}
			return super.scanUnsafe(key);
		}

		@Override
		public void onSubscribe(Subscription s) {
			set(s);
		}

		@Override
		public void onNext(T t) {
			if (won) {
				actual.onNext(t);
			}
			else if (parent.tryWin(index)) {
				won = true;
				actual.onNext(t);
			}
		}

		@Override
		public void onError(Throwable t) {
			if (won) {
				actual.onError(t);
			}
			else if (parent.tryWin(index)) {
				won = true;
				actual.onError(t);
			}
		}

		@Override
		public void onComplete() {
			if (won || parent.resignFromRace() == 0) {
				actual.onComplete();
			}
		}

		@Override
		public String stepName() {
			return "CloudFlux.firstNonEmpty";
		}

	}

}
